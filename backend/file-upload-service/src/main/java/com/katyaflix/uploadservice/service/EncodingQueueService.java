package com.katyaflix.uploadservice.service;

import com.katyaflix.uploadservice.dto.enums.FileType;
import com.katyaflix.uploadservice.dto.enums.FileUploadStatus;
import com.katyaflix.uploadservice.dto.enums.MediaCategory;
import com.katyaflix.uploadservice.dto.enums.PipelineStatus;
import com.katyaflix.uploadservice.dto.mediaenrichment.MediaEnrichment;
import com.katyaflix.uploadservice.dto.mediaenrichment.SubtitleEnrichment;
import com.katyaflix.uploadservice.dto.metadata.EpisodeMetadata;
import com.katyaflix.uploadservice.dto.metadata.MediaMetadata;
import com.katyaflix.uploadservice.dto.metadata.MovieMetadata;
import com.katyaflix.uploadservice.dto.metadata.SeasonMetadata;
import com.katyaflix.uploadservice.dto.metadata.ShowMetadata;
import com.katyaflix.uploadservice.entity.FileUpload;
import com.katyaflix.uploadservice.entity.UploadJob;
import com.katyaflix.uploadservice.messaging.MediaEnrichmentProducer;
import com.katyaflix.uploadservice.repository.UploadJobRepository;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static com.katyaflix.uploadservice.service.FileUploadStages.Stage.CATALOG_ENRICHMENT;
import static com.katyaflix.uploadservice.service.FileUploadStages.Stage.FILE_ENCODE;
import static com.katyaflix.uploadservice.service.FileUploadStages.error;
import static com.katyaflix.uploadservice.service.FileUploadStages.stage;

/**
 * Requires @EnableScheduling and an index on file_uploads (status, queued_at).
 *
 * Overall status:
 *   UPLOADING -> ENQUEUED -> ENCODING -> FINALIZING -> AWAITING_ENRICHMENT -> COMPLETED | FAILED
 * (the last hop is made by EnrichmentResultListener when the catalog replies)
 *
 * Per-stage detail lives in file_encode_state / catalog_enrichment_state (status, message, errors).
 */
@Service
public class EncodingQueueService {

    private static final Logger log = LoggerFactory.getLogger(EncodingQueueService.class);
    private static final Duration STALE_AFTER = Duration.ofMinutes(2);

    private final Path mediaRoot;
    private final MongoTemplate mongo;
    private final UploadJobRepository jobRepository;
    private final FileEncodingService encodingService;
    private final MediaEnrichmentProducer enrichmentProducer;
    private final int maxConcurrent;
    private final int maxAttempts;
    private final ExecutorService executor;
    private final AtomicInteger active = new AtomicInteger();

    public EncodingQueueService(
            Path mediaRoot,
            MongoTemplate mongo,
            UploadJobRepository jobRepository,
            FileEncodingService encodingService,
            MediaEnrichmentProducer enrichmentProducer,
            @Value("${encoding.max-concurrent:1}") int maxConcurrent,
            @Value("${encoding.max-attempts:3}") int maxAttempts
    ) {
        this.mediaRoot = mediaRoot.toAbsolutePath().normalize();
        this.mongo = mongo;
        this.jobRepository = jobRepository;
        this.encodingService = encodingService;
        this.enrichmentProducer = enrichmentProducer;
        this.maxConcurrent = maxConcurrent;
        this.maxAttempts = maxAttempts;
        this.executor = Executors.newFixedThreadPool(maxConcurrent);
    }

    // ---------- startup / shutdown ----------

    @EventListener(ApplicationReadyEvent.class)
    public void recoverOnStartup() {
        long n = mongo.updateMulti(
                Query.query(Criteria.where("status").in(FileUploadStatus.ENCODING, FileUploadStatus.FINALIZING)),
                requeueUpdate("Re-queued after service restart"),
                FileUpload.class).getModifiedCount();
        if (n > 0) log.warn("Re-queued {} file(s) that were in flight at startup", n);
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
        encodingService.killAll();
    }

    // ---------- polling ----------

    @Scheduled(fixedDelay = 2000)
    public void pollQueue() {
        try {
            reclaimStale();
            while (active.get() < maxConcurrent) {
                FileUpload claimed = claimNext();
                if (claimed == null) break;

                active.incrementAndGet();
                log.info("Claimed file {} (attempt {})", claimed.getId(), claimed.getAttempts());
                executor.submit(() -> {
                    try {
                        process(claimed);
                    } finally {
                        active.decrementAndGet();
                    }
                });
            }
        } catch (Exception e) {
            log.error("Encoding queue poll failed", e);
        }
    }

    private FileUpload claimNext() {
        Query q = Query.query(Criteria.where("status").is(FileUploadStatus.ENQUEUED))
                .with(Sort.by(Sort.Direction.ASC, "queued_at"))
                .limit(1);

        Instant now = Instant.now();
        Update u = new Update()
                .set("status", FileUploadStatus.ENCODING)
                .set("encoding_started_at", now)
                .set("heartbeat_at", now)
                .set("progress_percent", 0)
                .set("encoding_progress", 0)
                .inc("attempts", 1);
        stage(u, FILE_ENCODE, PipelineStatus.IN_PROGRESS, "Processing started");

        return mongo.findAndModify(q, u, FindAndModifyOptions.options().returnNew(true), FileUpload.class);
    }

    private void reclaimStale() {
        Instant cutoff = Instant.now().minus(STALE_AFTER);
        mongo.updateMulti(
                Query.query(Criteria.where("status").in(FileUploadStatus.ENCODING, FileUploadStatus.FINALIZING)
                        .and("heartbeat_at").lt(cutoff)),
                requeueUpdate("Re-queued: worker stopped responding"),
                FileUpload.class);
    }

    private Update requeueUpdate(String reason) {
        Update u = new Update()
                .set("status", FileUploadStatus.ENQUEUED)
                .set("queued_at", Instant.now());
        return stage(u, FILE_ENCODE, PipelineStatus.PENDING, reason);
    }

    // ---------- the actual work ----------

    private void process(FileUpload file) {
        Path source = Path.of(file.getSourcePath());
        try {
            UploadJob job = jobRepository.findById(file.getUploadJobId())
                    .orElseThrow(() -> new IllegalStateException("Upload job missing: " + file.getUploadJobId()));

            Target target = resolveTarget(job, file);
            String finalPath;

            if (file.getFileType() == FileType.VIDEO) {
                // 1. encode straight into the final directory
                MediaEnrichment enc = encodingService.encodeVideo(
                        target.mediaId(), source, target.dir(), pct -> heartbeat(file.getId(), pct));

                markFinalizing(file.getId(), "Encoding finished, preparing catalog update");

                MediaEnrichment out = new MediaEnrichment(
                        enc.id(), relative(enc.filePath()), file.getOriginalFileName(), enc.containerFormat(),
                        enc.videoCodec(), enc.audioCodec(), enc.resolution(), enc.durationSeconds(),
                        enc.fileSizeBytes(), enc.encodedAt());
                finalPath = out.filePath();

                // 2. mark AWAITING *before* publishing: the catalog can reply before we return,
                //    and the result listener only accepts files in AWAITING_ENRICHMENT.
                markAwaitingEnrichment(file.getId(), finalPath);
                enrichmentProducer.publishMediaEnrichment(file, target.category(), out);

            } else {
                SubtitleEnrichment enc = encodingService.convertSubtitle(
                        target.mediaId(), source, target.dir().resolve("subtitles"));

                markFinalizing(file.getId(), "Conversion finished, preparing catalog update");

                // detection returns "und" for short/unsupported text: fall back to what the client sent
                String lang = enc.languageCode();
                String label = enc.label();
                if ("und".equals(lang) && file.getLanguage() != null && !file.getLanguage().isBlank()) {
                    lang = file.getLanguage().toLowerCase();
                    label = lang;
                }

                SubtitleEnrichment out = new SubtitleEnrichment(
                        enc.id(), file.getOriginalFileName(), relative(enc.filePath()),
                        lang, label, enc.format(), enc.encodedAt());
                finalPath = out.filePath();

                markAwaitingEnrichment(file.getId(), finalPath);
                enrichmentProducer.publishSubtitleEnrichment(file, target.category(), out);
            }

            cleanupTemp(source.getParent());
            log.info("File {} encoded -> {}, waiting for catalog", file.getId(), finalPath);

        } catch (InterruptedException e) {
            // shutting down: put it back without burning an attempt
            Thread.currentThread().interrupt();
            mongo.updateFirst(byId(file.getId()),
                    requeueUpdate("Re-queued: service shutting down").inc("attempts", -1), FileUpload.class);
        } catch (Throwable t) {
            fail(file, t);
        }
    }

    private Target resolveTarget(UploadJob job, FileUpload file) {
        MediaMetadata metadata = job.getMediaMetadata();
        long tmdbId = file.getTmdbId();

        if (metadata instanceof MovieMetadata movie) {
            if (movie.catalogId() == null) {
                throw new IllegalStateException("Movie has no catalog id for job " + job.getId());
            }
            return new Target(MediaCategory.MOVIE, mediaRoot.resolve(movie.catalogId().toString()), movie.catalogId());
        }

        if (metadata instanceof ShowMetadata show) {
            for (SeasonMetadata season : show.seasons()) {
                for (EpisodeMetadata episode : season.episodes()) {
                    if (episode.tmdbId() != null && episode.tmdbId() == tmdbId) {
                        if (show.catalogId() == null || season.catalogId() == null || episode.catalogId() == null) {
                            throw new IllegalStateException("Missing catalog id for tmdb id " + tmdbId
                                    + " in job " + job.getId());
                        }
                        return new Target(MediaCategory.SHOW, mediaRoot
                                .resolve(show.catalogId().toString())
                                .resolve(season.catalogId().toString())
                                .resolve(episode.catalogId().toString()),
                                episode.catalogId());
                    }
                }
            }
            throw new IllegalStateException("tmdb id " + tmdbId + " is not an episode of job " + job.getId());
        }

        throw new IllegalStateException("Job " + job.getId() + " has no usable metadata");
    }

    /** mediaId = the catalog's movie or episode UUID (what the catalog resolves enrichment.id against). */
    private record Target(MediaCategory category, Path dir, UUID mediaId) {}

    private String relative(String absolutePath) {
        return mediaRoot.relativize(Path.of(absolutePath).toAbsolutePath().normalize())
                .toString().replace('\\', '/');
    }

    // ---------- status updates ----------

    private void markFinalizing(UUID id, String message) {
        Update u = new Update()
                .set("status", FileUploadStatus.FINALIZING)
                .set("heartbeat_at", Instant.now())
                .set("progress_percent", 100)
                .set("encoding_progress", 100);
        stage(u, FILE_ENCODE, PipelineStatus.COMPLETED, message);
        mongo.updateFirst(byId(id), u, FileUpload.class);
    }

    private void markAwaitingEnrichment(UUID id, String finalPath) {
        Update u = new Update()
                .set("status", FileUploadStatus.AWAITING_ENRICHMENT)
                .set("final_path", finalPath)
                .set("error", null);
        stage(u, CATALOG_ENRICHMENT, PipelineStatus.IN_PROGRESS, "Waiting for catalog service");
        mongo.updateFirst(byId(id), u, FileUpload.class);
    }

    private void heartbeat(UUID id, int percent) {
        Update u = new Update()
                .set("progress_percent", percent)
                .set("encoding_progress", percent)
                .set("heartbeat_at", Instant.now());
        stage(u, FILE_ENCODE, PipelineStatus.IN_PROGRESS, "Encoding " + percent + "%");
        mongo.updateFirst(byId(id), u, FileUpload.class);
    }

    private void fail(FileUpload file, Throwable t) {
        log.error("File {} failed on attempt {}", file.getId(), file.getAttempts(), t);
        String msg = String.valueOf(t.getMessage());

        if (file.getAttempts() >= maxAttempts) {
            Update u = new Update()
                    .set("status", FileUploadStatus.FAILED)
                    .set("error", msg);
            stage(u, FILE_ENCODE, PipelineStatus.FAILED,
                    "Failed after " + file.getAttempts() + " attempt(s): " + msg);
            mongo.updateFirst(byId(file.getId()), u, FileUpload.class);
        } else {
            Update u = requeueUpdate("Attempt " + file.getAttempts() + " failed, re-queued: " + msg)
                    .set("error", msg);
            error(u, FILE_ENCODE, "Attempt " + file.getAttempts() + ": " + msg);
            mongo.updateFirst(byId(file.getId()), u, FileUpload.class);
        }
    }

    private static Query byId(UUID id) {
        return Query.query(Criteria.where("_id").is(id));
    }

    private void cleanupTemp(Path dir) {
        try {
            FileSystemUtils.deleteRecursively(dir);
        } catch (IOException e) {
            log.warn("Could not clean temp dir {}", dir, e);
        }
    }
}