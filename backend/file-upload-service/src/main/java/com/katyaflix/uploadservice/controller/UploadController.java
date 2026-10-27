package com.katyaflix.uploadservice.controller;

import com.katyaflix.uploadservice.dto.enums.MediaCategory;
import com.katyaflix.uploadservice.dto.metadata.MediaMetadata;
import com.katyaflix.uploadservice.dto.metadata.MovieMetadata;
import com.katyaflix.uploadservice.dto.metadata.ShowMetadata;
import com.katyaflix.uploadservice.entity.FileUpload;
import com.katyaflix.uploadservice.entity.UploadJob;
import com.katyaflix.uploadservice.messaging.CatalogValidationProducer;
import com.katyaflix.uploadservice.service.TmdbService;
import com.katyaflix.uploadservice.service.UploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/uploads")
public class UploadController {

    private static final Logger log = LoggerFactory.getLogger(UploadController.class);

    private final UploadService uploadService;
    private final TmdbService tmdbService;
    private final CatalogValidationProducer catalogValidationProducer;

    public UploadController(
            UploadService uploadService,
            TmdbService tmdbService,
            CatalogValidationProducer catalogValidationProducer
    ) {
        this.uploadService = uploadService;
        this.tmdbService = tmdbService;
        this.catalogValidationProducer = catalogValidationProducer;
    }

    // first upload click posts to create a new upload job.
    @PostMapping("/upload-job")
    public ResponseEntity<?> newUploadJob(
            @RequestParam("tmdb_id") String tmdbId,
            @RequestParam("media_type") String type, // show or movie
            @RequestParam("userId") UUID userId       // TODO: take from the auth principal instead
    ) {
        if (type == null || type.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "media_type is required."));
        }

        MediaCategory mediaType;
        if ("MOVIE".equalsIgnoreCase(type)) {
            mediaType = MediaCategory.MOVIE;
        } else if ("SHOW".equalsIgnoreCase(type)) {
            mediaType = MediaCategory.SHOW;
        } else {
            return ResponseEntity.badRequest().body(Map.of("message", "media_type must be SHOW or MOVIE."));
        }

        long parsedTmdbId;
        try {
            parsedTmdbId = Long.parseLong(tmdbId);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "tmdb_id must be a number."));
        }

        MediaMetadata metadata = tmdbService.populateMedia(mediaType, parsedTmdbId);
        UploadJob job = uploadService.createJob(metadata, userId);
        if (job == null) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Could not create upload job."));
        }

        if (mediaType == MediaCategory.MOVIE) {
            catalogValidationProducer.publishMovieValidation(job, (MovieMetadata) metadata);
        } else {
            catalogValidationProducer.publishShowValidation(job, (ShowMetadata) metadata);
        }

        return ResponseEntity.ok(Map.of("job_id", job.getId().toString()));
    }

    /**
     * Accepts one file, stores it in temp, queues it for encoding and returns immediately (202).
     * The client polls GET /uploads/files/{id} for progress.
     */
    @PutMapping(path = "/upload/{uploadJobId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(
            @PathVariable UUID uploadJobId,
            @RequestParam("tmdb_id") String tmdbId,
            @RequestParam(value = "media_type", required = false) String type, // no longer needed, kept so existing clients don't break
            @RequestParam(value = "language", required = false) String language, // subtitles only, e.g. "en"
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") UUID userId
    ) {
        long parsedTmdbId;
        try {
            parsedTmdbId = Long.parseLong(tmdbId);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "tmdb_id must be a number."));
        }

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "file is required."));
        }

        // missing job OR someone else's job: same 404 so job ids can't be probed
        Optional<UploadJob> jobOpt = uploadService.findJob(uploadJobId);
        if (jobOpt.isEmpty() || !userId.equals(jobOpt.get().getUserId())) {
            return ResponseEntity.notFound().build();
        }
        UploadJob job = jobOpt.get();

        if (!uploadService.isJobValid(uploadJobId)) {
            return ResponseEntity.status(409)
                    .body(Map.of("message", "Catalog validation is not complete (or failed) for this job."));
        }

        if (!uploadService.jobContainsTmdbId(job, parsedTmdbId)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "tmdb_id does not belong to this upload job."));
        }

        try {
            FileUpload fileUpload = uploadService.acceptFile(uploadJobId, parsedTmdbId, language, file);
            return ResponseEntity.accepted().body(Map.of(
                    "file_upload_id", fileUpload.getId().toString(),
                    "status", fileUpload.getStatus().toString()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IOException e) {
            log.error("Failed to store upload for job {}", uploadJobId, e);
            return ResponseEntity.internalServerError().body(Map.of("message", "Failed to store file."));
        }
    }

    /** Poll this after the PUT above. */
    @GetMapping("/files/{fileUploadId}")
    public ResponseEntity<?> fileStatus(
            @PathVariable UUID fileUploadId,
            @RequestParam("userId") UUID userId // TODO: take from the auth principal instead
    ) {
        Optional<FileUpload> fileOpt = uploadService.findFileUpload(fileUploadId);
        if (fileOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        FileUpload f = fileOpt.get();

        boolean owned = uploadService.findJob(f.getUploadJobId())
                .map(j -> userId.equals(j.getUserId()))
                .orElse(false);
        if (!owned) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(new FileStatusResponse(
                f.getId(),
                f.getUploadJobId(),
                f.getStatus().toString(),
                f.getProgressPercent(),
                f.getAttempts(),
                f.getError(),
                f.getFinalPath()));
    }

    public record FileStatusResponse(
            UUID file_upload_id,
            UUID upload_job_id,
            String status,
            int progress_percent,
            int attempts,
            String error,
            String final_path) {}




    @GetMapping("/upload-job/{jobId}")
    public ResponseEntity<?> jobStatus(
            @PathVariable UUID jobId,
            @RequestParam("userId") UUID userId // TODO: auth principal
    ) {
        Optional<UploadJob> opt = uploadService.findJob(jobId);
        if (opt.isEmpty() || !userId.equals(opt.get().getUserId())) {
            return ResponseEntity.notFound().build();
        }
        UploadJob job = opt.get();

        List<EpisodeRef> episodes = List.of();
        if (job.getMediaMetadata() instanceof ShowMetadata show) {
            episodes = show.seasons().stream()
                    .flatMap(season -> season.episodes().stream()
                            .map(ep -> new EpisodeRef(season.seasonNumber(), ep.episodeNumber(), ep.tmdbId())))
                    .toList();
        }

        return ResponseEntity.ok(new JobStatusResponse(
                job.getId(),
                job.getMediaType(),
                job.getTitle(),
                job.getTmdbId(),
                job.getValidationStatus().name(),
                "Message here...",
                episodes));
    }

    // JSON field names are the record component names, so they're snake_case on purpose.
    public record EpisodeRef(int season_number, int episode_number, long tmdb_id) {}

    public record JobStatusResponse(
            UUID id,
            String media_type,
            String title,
            long tmdb_id,
            String validation_status,
            String validation_message,
            List<EpisodeRef> episodes) {}


}