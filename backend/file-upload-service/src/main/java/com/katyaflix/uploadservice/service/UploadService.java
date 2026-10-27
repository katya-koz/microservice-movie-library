package com.katyaflix.uploadservice.service;

import com.katyaflix.uploadservice.dto.enums.FileType;
import com.katyaflix.uploadservice.dto.enums.FileUploadStatus;
import com.katyaflix.uploadservice.dto.enums.MediaCategory;
import com.katyaflix.uploadservice.dto.enums.PipelineStatus;
import com.katyaflix.uploadservice.dto.metadata.*;
import com.katyaflix.uploadservice.entity.FileUpload;
import com.katyaflix.uploadservice.entity.UploadJob;
import com.katyaflix.uploadservice.repository.FileUploadRepository;
import com.katyaflix.uploadservice.repository.UploadJobRepository;
import com.katyaflix.uploadservice.util.FileTypeUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Job bookkeeping, asset downloads and accepting raw uploads.
 * Encoding is NOT done here: acceptFile() only stores the file in temp and marks the
 * FileUpload ENQUEUED; EncodingQueueService picks it up from there.
 */
@Service
@RequiredArgsConstructor
public class UploadService {
    private static final Logger log = LoggerFactory.getLogger(UploadService.class);

    private final FileUploadRepository fileUploadRepository;
    private final UploadJobRepository uploadJobRepository;
    private final TmdbImageService tmdbImageService;
    private final Path mediaRoot;
    private final Path tempMediaRoot;

    // create job row
    public UploadJob createJob(MediaMetadata metadata, UUID userId) {
        UploadJob job = new UploadJob();
        job.setMediaType(metadata.type().toString());
        job.setTmdbId(metadata.tmdbId());

        if (metadata.type() == MediaCategory.MOVIE) {
            MovieMetadata movie = (MovieMetadata) metadata;
            job.setTitle(movie.title());
            job.setMediaMetadata(movie);
        } else if (metadata.type() == MediaCategory.SHOW) {
            ShowMetadata show = (ShowMetadata) metadata;
            job.setTitle(show.title());
            job.setMediaMetadata(show);
        } else {
            log.error("Job creation failed. Unsupported media type {}", metadata.type());
            return null;
        }

        job.setUserId(userId);
        job.setUpdatedAt(Instant.now());
        job.setCreatedAt(Instant.now());

        return uploadJobRepository.save(job);
    }

    public boolean isJobValid(UUID uploadJobId) {
        return uploadJobRepository.findById(uploadJobId)
                .map(job -> job.getValidationStatus() == PipelineStatus.COMPLETED)
                .orElse(false);
    }

    public boolean downloadMovieAssets(MovieMetadata metadata) throws IOException {
        Path assetDir = mediaRoot.resolve("assets").resolve("movies").resolve(String.valueOf(metadata.catalogId()));

        Files.createDirectories(assetDir);
        tmdbImageService.download(metadata.posterPath(), assetDir.resolve("poster.jpg"), mediaRoot);
        tmdbImageService.download(metadata.backdropPath(), assetDir.resolve("backdrop.jpg"), mediaRoot);

        return true;
    }

    public boolean downloadShowAssets(ShowMetadata metadata) throws IOException {
        Path showAssetDir = mediaRoot.resolve("assets").resolve("shows").resolve(String.valueOf(metadata.catalogId()));

        Files.createDirectories(showAssetDir);
        tmdbImageService.download(metadata.posterPath(), showAssetDir.resolve("poster.jpg"), mediaRoot);
        tmdbImageService.download(metadata.backdropPath(), showAssetDir.resolve("backdrop.jpg"), mediaRoot);

        // assets/shows/{showId}/seasons/{seasonId}/episodes/{episodeId}
        for (SeasonMetadata season : metadata.seasons()) {
            Path seasonAssetDir = showAssetDir.resolve("seasons").resolve(String.valueOf(season.catalogId()));
            Files.createDirectories(seasonAssetDir);
            // NOTE: was tempMediaRoot; the dirs live under mediaRoot, so use mediaRoot here too
            tmdbImageService.download(season.posterPath(), seasonAssetDir.resolve("poster.jpg"), mediaRoot);

            for (EpisodeMetadata episode : season.episodes()) {
                Path episodeAssetDir = seasonAssetDir.resolve("episodes").resolve(String.valueOf(episode.catalogId()));
                Files.createDirectories(episodeAssetDir);
                tmdbImageService.download(episode.stillPath(), episodeAssetDir.resolve("still.jpg"), mediaRoot);
            }
        }
        return true;
    }

    public Optional<UploadJob> findJob(UUID jobId) {
        return uploadJobRepository.findById(jobId);
    }

    public Optional<FileUpload> findFileUpload(UUID fileUploadId) {
        return fileUploadRepository.findById(fileUploadId);
    }

    /** Is this tmdb id the job's movie, or one of its show's episodes? */
    public boolean jobContainsTmdbId(UploadJob job, long tmdbId) {
        MediaMetadata metadata = job.getMediaMetadata();

        if (metadata instanceof MovieMetadata) {
            return job.getTmdbId() == tmdbId;
        }
        if (metadata instanceof ShowMetadata show) {
            return show.seasons().stream()
                    .flatMap(season -> season.episodes().stream())
                    .anyMatch(episode -> episode.tmdbId() != null && episode.tmdbId() == tmdbId);
        }
        return false;
    }

    /**
     * Saves the raw upload into temp under a server-generated path and queues it
     * for encoding. Never uses the client's file name for any path.
     *
     * The row is inserted as UPLOADING first (so we have its id and the queue can't see
     * a half-written file), then flipped to ENQUEUED once the file is on disk.
     */
    public FileUpload acceptFile(UUID jobId, long tmdbId, String language, MultipartFile file) throws IOException {
        String original = file.getOriginalFilename();
        String ext = original == null ? "" : FilenameUtils.getExtension(original).toLowerCase();
        if (!ext.matches("[a-z0-9]{1,8}")) {
            throw new IllegalArgumentException("Missing or invalid file extension.");
        }

        FileType type = FileTypeUtil.getFileType(ext);
        if (type == null) {
            throw new IllegalArgumentException("Unsupported file type: ." + ext);
        }

        FileUpload fu = new FileUpload();
        fu.setUploadJobId(jobId);
        fu.setTmdbId(tmdbId);
        fu.setFileType(type);
        fu.setOriginalFileName(original);   // metadata only
        fu.setFileExtension(ext);
        if (type == FileType.SUBTITLE) {
            fu.setLanguage(language);
        }
        fu.setStatus(FileUploadStatus.UPLOADING);
        fu = fileUploadRepository.save(fu);

        Path dir = tempMediaRoot.resolve("uploads").resolve(fu.getId().toString());
        Path source = dir.resolve("source." + ext);
        try {
            Files.createDirectories(dir);
            file.transferTo(source);
        } catch (IOException | RuntimeException e) {
            fu.setStatus(FileUploadStatus.FAILED);
            fu.setError("Upload failed: " + e.getMessage());
            fileUploadRepository.save(fu);
            FileSystemUtils.deleteRecursively(dir);
            throw e;
        }

        fu.setSourcePath(source.toString());
        fu.setQueuedAt(Instant.now());
        fu.setStatus(FileUploadStatus.ENQUEUED);   // from here the queue owns it
        return fileUploadRepository.save(fu);
    }
}