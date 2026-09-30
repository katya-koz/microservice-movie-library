package com.katyaflix.uploadservice.service;

import com.katyaflix.uploadservice.dto.EncodedMediaFile;
import com.katyaflix.uploadservice.dto.FileUploadMetadata;
import com.katyaflix.uploadservice.entity.UploadJob;
import com.katyaflix.uploadservice.messaging.CatalogValidationProducer;
import com.katyaflix.uploadservice.repository.UploadJobRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UploadService {
    private static final Logger log = LoggerFactory.getLogger(UploadService.class);

    private final UploadJobRepository jobRepository;
    private final TmdbImageService tmdbImageService;
    private final Path mediaRoot;
    private final Path tempMediaRoot;
    private final FileFinalizationService fileFinalizationService;
    private final CatalogValidationProducer catalogValidationProducer;
    private final UploadJobRepository uploadJobRepository;
    private final FileEncodingService fileEncodingService;

    public UploadService(
            UploadJobRepository jobRepository,
            TmdbImageService tmdbImageService,
            @Value("${media.root}") String mediaRoot,
            FileFinalizationService fileFinalizationService,
            CatalogValidationProducer catalogValidationProducer,
            UploadJobRepository uploadJobRepository,
            FileEncodingService fileEncodingService
    ) {
        this.jobRepository = jobRepository;
        this.tmdbImageService = tmdbImageService;
        this.mediaRoot = Path.of(mediaRoot);
        this.tempMediaRoot = Path.of(mediaRoot).resolve("temp");
        this.fileFinalizationService = fileFinalizationService;
        this.catalogValidationProducer = catalogValidationProducer;

        this.uploadJobRepository = uploadJobRepository;
        this.fileEncodingService = fileEncodingService;
    }

    /**
     * The set of files saved to disk for a job, split by type. Video files
     * go through the normal ffmpeg transcode pipeline; subtitle files go
     * through the much lighter WebVTT conversion. Keeping them apart from
     * the point they're saved onward is what fixes uploads that include a
     * subtitle file crashing ffmpeg (it was previously being handed .srt
     * files and told to -map a video/audio stream that doesn't exist).
     */
    private record SavedFiles(Map<String, Path> videoFiles, Map<String, Path> subtitleFiles) {}

    // create job row immedietly
    public UploadJob createJob(FileUploadMetadata.UploadMetadataDto metadata, UUID userId) {
        UploadJob job = new UploadJob();

        job.setMediaType(metadata.type().toString());
        job.setStatus(UploadJob.UploadStatus.PENDING);
        job.setTmdbId(metadata.tmdbId());

        // catalogPayload is  typed as the sealed UploadMetadataDto itself
        // Mongo stores movie and show metadata natively in the same collection
        if (metadata.type() == FileUploadMetadata.UploadType.MOVIE) {
            FileUploadMetadata.MovieUploadMetadataDto movie = (FileUploadMetadata.MovieUploadMetadataDto) metadata;
            job.setTitle(movie.title());
            job.setCatalogPayload(movie);

        } else {
            FileUploadMetadata.ShowUploadMetadataDto show = (FileUploadMetadata.ShowUploadMetadataDto) metadata;
            job.setTitle(show.title());
            job.setCatalogPayload(show);
        }

        job.setUserId(userId);
        job.setUpdatedAt(Instant.now());
        job.setCreatedAt(Instant.now());

        return jobRepository.save(job);
    }


    public void finalizeFileUpload(UploadJob job) {
        if (!Boolean.TRUE.equals(job.getCatalogValidationStatus()) || !Boolean.TRUE.equals(job.getFileUploadStatus())) {
            throw new IllegalStateException( "finalizeFileUpload called before both sides completed for job " + job.getId());
        }

        jobRepository.updateStatusWhereId(job.getId(), UploadJob.UploadStatus.FINALIZING);
        try {

            if ("MOVIE".equalsIgnoreCase(job.getMediaType())) {
                fileFinalizationService.finalizeMovie(job);
            } else {
                fileFinalizationService.finalizeShow(job);
            }

        } catch (Exception e) {
            log.error( "File finalization failed for job {}", job.getId(), e );
            fail(job, e.getMessage());
        }
    }
    @Transactional
    public void completeFileUpload(UUID jobId) {
        jobRepository.updateStatusWhereId(jobId, UploadJob.UploadStatus.COMPLETED);
        jobRepository.updateFilePathUpdateStatusWhereId(jobId, true);
        jobRepository.updateCompletedAtWhereId(jobId);
    }

    /**
     * Saves the uploaded files to a temp directory and drops the job into
     * the mongo-backed encoding queue. Actual encoding happens later, off
     * the request thread, once EncodingQueueService has a free slot (see
     * encoding.max-concurrent-jobs) - see #runEncodingForJob.
     */
    public void processMovie(UploadJob job, FileUploadMetadata.MovieUploadMetadataDto metadata,  Map<String, MultipartFile> fileMap) {
        try {
            jobRepository.updateStatusWhereId(job.getId(), UploadJob.UploadStatus.PENDING);

            // Ask catalog service to validate/upsert the movie and return its UUID.
            catalogValidationProducer.publishMovieValidation(job, metadata);
            jobRepository.updateStatusWhereId(job.getId(), UploadJob.UploadStatus.SAVING_ASSETS);
            /*
             * Temporary media:
             *
             * temp/movies/{tmdbId}/{filename}
             */
            Path movieMediaDir = tempMediaRoot.resolve("movies").resolve( String.valueOf(metadata.tmdbId()));

            SavedFiles saved = saveMovieFiles(metadata, fileMap, movieMediaDir, job);

            enqueueForEncoding(job.getId(), saved);

        } catch (Exception e) {
            log.error("Upload processing failed for job {}", job.getId(), e);

            jobRepository.updateStatusWhereId(job.getId(), UploadJob.UploadStatus.FAILURE);
            jobRepository.appendErrorMessage(job.getId(), e.getMessage());

            throw new UploadProcessingException(  "Upload processing failed: " + e.getMessage(), e);
        }
    }

    public void processShow(UploadJob job,
                            FileUploadMetadata.ShowUploadMetadataDto metadata,
                            Map<String, MultipartFile> fileMap) {
        try {
            jobRepository.updateStatusWhereId(job.getId(), UploadJob.UploadStatus.PENDING);

            // Ask catalog service to validate/upsert the show, seasons and episodes.
            catalogValidationProducer.publishShowValidation(job, metadata);

            /*
             * Temporary media:
             *
             * temp/shows/{showTmdbId}/...
             */
            Path showMediaDir = tempMediaRoot.resolve("shows").resolve(
                    String.valueOf(metadata.tmdbId()));



            jobRepository.updateStatusWhereId(job.getId(), UploadJob.UploadStatus.SAVING_ASSETS);
            SavedFiles saved = saveShowFiles(metadata, fileMap, showMediaDir, job);

            enqueueForEncoding(job.getId(), saved);

        } catch (Exception e) {
            log.error("Upload processing failed for job {}", job.getId(), e);

            fail(job, e.getMessage());

            throw new UploadProcessingException(
                    "Upload processing failed: " + e.getMessage(), e);
        }
    }

    private void enqueueForEncoding(UUID jobId, SavedFiles saved) {
        jobRepository.enqueueForEncoding(jobId,UploadJob.UploadStatus.QUEUED_FOR_ENCODING,toStringMap(saved.videoFiles()), toStringMap(saved.subtitleFiles()));
    }

    private Map<String, String> toStringMap(Map<String, Path> paths) {
        Map<String, String> result = new HashMap<>();
        paths.forEach((key, path) -> result.put(key, path.toString()));
        return result;
    }

    /**
     * Runs the actual ffmpeg work for a job that EncodingQueueService has
     * claimed off the queue, then continues the same post-encoding flow that
     * used to run inline inside processMovie/processShow. On failure, the
     * job is marked FAILURE and its slot in the encoding queue is freed
     * automatically (the queue only ever counts jobs whose status is
     * ENCODING).
     */
    public void runEncodingForJob(UploadJob job) {
        UUID jobId = job.getId();
        try {
            Map<String, Path> videoFiles = toPathMap(job.getVideoFilePaths());
            Map<String, Path> subtitleFiles = toPathMap(job.getSubtitleFilePaths());

            jobRepository.updateCurrentStepWhereId(jobId,"Encoding " + videoFiles.size() + " video file(s)");

            List<EncodedMediaFile> encodedFiles = fileEncodingService.encodeAll(videoFiles);

            if (!subtitleFiles.isEmpty()) {
                jobRepository.updateCurrentStepWhereId(jobId,"Converting " + subtitleFiles.size() + " subtitle file(s)");
                fileEncodingService.encodeSubtitles(subtitleFiles);
            }

            jobRepository.saveEncodedMediaFiles(jobId, encodedFiles);
            jobRepository.updateFileUploadStatusWhereId(jobId, true);
            jobRepository.updateCurrentStepWhereId(jobId, "Encoding complete");

            advance(jobId);

        } catch (Exception e) {
            log.error("Encoding failed for job {}", jobId, e);
            jobRepository.updateStatusWhereId(jobId, UploadJob.UploadStatus.FAILURE);
            jobRepository.appendErrorMessage(jobId, e.getMessage());
        }
    }

    private Map<String, Path> toPathMap(Map<String, String> raw) {
        if (raw == null) {
            return Map.of();
        }
        Map<String, Path> result = new HashMap<>();
        raw.forEach((key, value) -> result.put(key, Path.of(value)));
        return result;
    }

    public void advance(UUID jobId) {
        UploadJob job = uploadJobRepository.findById(jobId) .orElseThrow(() -> new IllegalStateException("Upload job not found: " + jobId));
        System.out.println(job.getFileUploadStatus() + " " + job.getCatalogValidationStatus());
        if (Boolean.TRUE.equals(job.getFileUploadStatus()) && Boolean.TRUE.equals(job.getCatalogValidationStatus())) {

            for( EncodedMediaFile encoded : job.getEncodedMediaFiles()){
                if(job.getMediaType().equals("MOVIE")){
                    FileUploadMetadata.MovieUploadMetadataDto payload = (FileUploadMetadata.MovieUploadMetadataDto) job.getCatalogPayload();

                    fileEncodingService.sendMediaEnrichmentEvent(job.getId(), payload.id(), null, "n/a", encoded);
                }else{
                    FileUploadMetadata.ShowUploadMetadataDto payload = (FileUploadMetadata.ShowUploadMetadataDto) job.getCatalogPayload();

                    fileEncodingService.sendMediaEnrichmentEvent(job.getId(), null, payload.getEpisodeByTmdbId(Long.valueOf(encoded.tmdbId())).id(), "n/a", encoded);
                }

            }
            finalizeFileUpload(job);

        } else {
            jobRepository.updateStatusWhereId(job.getId(), UploadJob.UploadStatus.AWAITING_CATALOG);
        }
    }

    public void fail(UploadJob job, String message) {
        jobRepository.updateFileUploadStatusWhereId(job.getId(), false);
        jobRepository.updateStatusWhereId(job.getId(), UploadJob.UploadStatus.FAILURE);
        jobRepository.appendErrorMessage(job.getId(), message);
    }

    private SavedFiles saveMovieFiles(
            FileUploadMetadata.MovieUploadMetadataDto metadata,
            Map<String, MultipartFile> fileMap, Path movieMediaDir, UploadJob job)
            throws IOException {

        List<FileUploadMetadata.UploadFileEntryDto> files = metadata.files();

        Map<String, Path> videoFiles = new HashMap<>();
        Map<String, Path> subtitleFiles = new HashMap<>();

        /*
         * Assets:
         *
         * temp/assets/movies/{movieTmdbId}/
         */
        Path assetDir = tempMediaRoot.resolve("assets").resolve("movies").resolve(  String.valueOf(metadata.tmdbId()));

        Files.createDirectories(assetDir);

        tmdbImageService.download( metadata.posterPath(), assetDir.resolve("poster.jpg"), tempMediaRoot);

        tmdbImageService.download(metadata.backdropPath(), assetDir.resolve("backdrop.jpg"), tempMediaRoot);

        /*
         * Media:
         *
         * temp/movies/{movieTmdbId}/{filename}          <- video
         * temp/movies/{movieTmdbId}/subtitles/{filename} <- subtitles
         *
         * Subtitles are kept in their own subdirectory so that
         * FileFinalizationService#findSingleMediaFile (which only looks at
         * regular files directly inside this directory) keeps seeing
         * exactly one file: the video.
         */
        Files.createDirectories(movieMediaDir);
        Path subtitleDir = movieMediaDir.resolve("subtitles");

        for (FileUploadMetadata.UploadFileEntryDto entry : files) {
            MultipartFile multipartFile = fileMap.get(entry.key());

            if (multipartFile == null) {
                throw new IllegalArgumentException("Missing uploaded file for key: " + entry.key());
            }

            String filename = Paths.get(Objects.requireNonNull(multipartFile.getOriginalFilename())).getFileName().toString();

            boolean isSubtitle = entry.fileType() == FileUploadMetadata.FileType.SUBTITLE;
            Path targetDir = isSubtitle ? subtitleDir : movieMediaDir;

            if (isSubtitle) {
                Files.createDirectories(subtitleDir);
            }

            Path destination = targetDir.resolve(filename);
            Files.deleteIfExists(destination);
            multipartFile.transferTo(destination);

            (isSubtitle ? subtitleFiles : videoFiles).put(entry.key(), destination);
        }

        return new SavedFiles(videoFiles, subtitleFiles);
    }

    private SavedFiles saveShowFiles(FileUploadMetadata.ShowUploadMetadataDto metadata,Map<String, MultipartFile> fileMap, Path showMediaDir, UploadJob job) throws IOException
    {
        Map<String, Path> videoFiles = new HashMap<>();
        Map<String, Path> subtitleFiles = new HashMap<>();

        /*
         * Show assets:
         *
         * temp/assets/shows/{showTmdbId}/
         */
        Path showAssetDir =
                tempMediaRoot.resolve("assets").resolve("shows").resolve(String.valueOf(metadata.tmdbId()));

        Files.createDirectories(showAssetDir);

        tmdbImageService.download(metadata.posterPath(),showAssetDir.resolve("poster.jpg"), tempMediaRoot);

        tmdbImageService.download(metadata.backdropPath(),showAssetDir.resolve("backdrop.jpg"), tempMediaRoot);

        /*
         * Show media:
         *
         * temp/shows/{showTmdbId}/
         */
        Files.createDirectories(showMediaDir);

        for (FileUploadMetadata.SeasonUploadMetadataDto season : metadata.seasons()) {
            /*
             * Assets:
             *
             * temp/assets/shows/{showTmdbId}/
             *     seasons/{seasonTmdbId}/
             */
            Path seasonAssetDir = showAssetDir.resolve("seasons").resolve( String.valueOf(season.tmdbId()));

            Files.createDirectories(seasonAssetDir);

            tmdbImageService.download(season.posterPath(), seasonAssetDir.resolve("poster.jpg"), tempMediaRoot);

            for (FileUploadMetadata.EpisodeUploadMetadataDto episode : season.episodes()) {
                /*
                 * Assets:
                 *
                 * temp/assets/shows/{showTmdbId}/
                 *     seasons/{seasonTmdbId}/
                 *         episodes/{episodeTmdbId}/
                 */
                Path episodeAssetDir = seasonAssetDir.resolve("episodes") .resolve(String.valueOf(episode.tmdbId()));

                Files.createDirectories(episodeAssetDir);

                tmdbImageService.download(episode.stillPath(), episodeAssetDir.resolve("still.jpg"), tempMediaRoot);

                /*
                 * Media:
                 *
                 * temp/shows/{showTmdbId}/
                 *     seasons/{seasonTmdbId}/
                 *         episodes/{episodeTmdbId}/{filename}           <- video
                 *         episodes/{episodeTmdbId}/subtitles/{filename} <- subtitles
                 */
                Path episodeMediaDir = showMediaDir.resolve("seasons").resolve(String.valueOf(season.tmdbId())).resolve("episodes").resolve(String.valueOf(episode.tmdbId()));

                Files.createDirectories(episodeMediaDir);
                Path episodeSubtitleDir = episodeMediaDir.resolve("subtitles");

                for (FileUploadMetadata.UploadFileEntryDto entry : episode.files()) {
                    MultipartFile multipartFile = fileMap.get(entry.key());

                    if (multipartFile == null) {
                        throw new IllegalArgumentException( "Missing uploaded file for key: " + entry.key());
                    }

                    String filename = Paths .get(Objects.requireNonNull( multipartFile.getOriginalFilename())).getFileName().toString();

                    boolean isSubtitle = entry.fileType() == FileUploadMetadata.FileType.SUBTITLE;
                    Path targetDir = isSubtitle ? episodeSubtitleDir : episodeMediaDir;

                    if (isSubtitle) {
                        Files.createDirectories(episodeSubtitleDir);
                    }

                    Path destination = targetDir.resolve(filename);
                    Files.deleteIfExists(destination);
                    multipartFile.transferTo(destination);

                    (isSubtitle ? subtitleFiles : videoFiles).put(entry.key(), destination);
                }
            }
        }

        return new SavedFiles(videoFiles, subtitleFiles);
    }



    @Transactional
    public void appendErrorMessageById(UUID uuid, String message) {

        jobRepository.appendErrorMessage(uuid, message);
    }
    @Transactional
    public void updateMediaEnrichmentStatusById(UUID uuid, boolean b) {

        jobRepository.updateMediaEnrichmentStatusWhereId(uuid, b);
    }
}