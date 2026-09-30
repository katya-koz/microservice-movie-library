package com.katyaflix.uploadservice.controller;

import com.katyaflix.uploadservice.dto.FileUploadMetadata;
import com.katyaflix.uploadservice.dto.UploadJobStatusDto;
import com.katyaflix.uploadservice.messaging.CatalogValidationProducer;
import com.katyaflix.uploadservice.repository.UploadJobRepository;
import com.katyaflix.uploadservice.service.TmdbService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.katyaflix.uploadservice.entity.UploadJob;
import com.katyaflix.uploadservice.service.UploadProcessingException;
import com.katyaflix.uploadservice.service.UploadService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/uploads")
public class UploadController {

    private final ObjectMapper objectMapper;
    private final UploadService uploadService;
    private final TmdbService tmdbService;
    private final CatalogValidationProducer catalogValidationProducer;
    private final UploadJobRepository uploadJobRepository;

    public UploadController(
            ObjectMapper objectMapper,
            UploadService uploadService,
            TmdbService tmdbService,
            CatalogValidationProducer catalogValidationProducer,
            UploadJobRepository uploadJobRepository
    )
    {
        this.objectMapper = objectMapper;
        this.uploadService = uploadService;
        this.tmdbService = tmdbService;
        this.catalogValidationProducer = catalogValidationProducer;
        this.uploadJobRepository = uploadJobRepository;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(
            @RequestParam("metadata") String metadataJson,
            MultipartHttpServletRequest request,
            @RequestParam("userId") UUID userId
    ) {
        JsonNode metadataNode;
        try {
            metadataNode = objectMapper.readTree(metadataJson);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Malformed metadata JSON."));
        }

        JsonNode typeNode = metadataNode.get("type");
        if (typeNode == null || typeNode.asText().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "metadata.type is required."));
        }
        String type = typeNode.asText();

        FileUploadMetadata.UploadMetadataDto metadata;
        try {
            if ("MOVIE".equalsIgnoreCase(type)) {
                metadata = objectMapper.treeToValue(metadataNode, FileUploadMetadata.MovieUploadMetadataDto.class);
            } else if ("SHOW".equalsIgnoreCase(type)) {
                metadata = objectMapper.treeToValue(metadataNode, FileUploadMetadata.ShowUploadMetadataDto.class);
            } else {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid metadata type: " + type));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "metadata did not match the expected shape: " + e.getMessage()));
        }

        if (metadata.getFiles() == null || metadata.getFiles().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "No files listed in metadata."));
        }

        // populate information from tmdb api instead of trusting front end metadata :3
        metadata = tmdbService.populateMedia(metadata);

        UploadJob job = uploadService.createJob(metadata, userId);


        if (metadata instanceof FileUploadMetadata.MovieUploadMetadataDto movieDto) {
            catalogValidationProducer.publishMovieValidation(job, movieDto);
        } else if (metadata instanceof FileUploadMetadata.ShowUploadMetadataDto showDto) {
            catalogValidationProducer.publishShowValidation(job, showDto);
        }

        try {
            if ("MOVIE".equalsIgnoreCase(type)) {
                uploadService.processMovie(job, (FileUploadMetadata.MovieUploadMetadataDto) metadata, request.getFileMap());
            } else {
                uploadService.processShow(job, (FileUploadMetadata.ShowUploadMetadataDto) metadata, request.getFileMap());
            }

        } catch (UploadProcessingException e) {
            String message = e.getMessage() != null ? e.getMessage() : "Unknown error during upload processing.";
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("jobId", job.getId().toString(), "status", "FAILED", "message", message));
        }

        return ResponseEntity.ok(Map.of("jobId", job.getId().toString(), "status", job.getStatus()));
    }

    /**
     * Backs the "My Upload Jobs" page: a paginated, newest-first list of
     * every job (no per-user scoping yet - UploadJob.requestedBy isn't
     * populated anywhere in this codebase yet either).
     */
    @GetMapping("/jobs")
    public ResponseEntity<Page<UploadJobStatusDto>> listJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UUID userId
    ) {
        Pageable pageable = PageRequest.of(
                page,
                Math.min(size, 100),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<UploadJob> jobs = userId != null
                ? uploadJobRepository.findAllByUserIdOrderByCreatedAtDesc(userId, pageable)
                : uploadJobRepository.findAllByOrderByCreatedAtDesc(pageable);

        return ResponseEntity.ok(jobs.map(UploadJobStatusDto::from));
    }
    /**
     * Single job detail - used for the initial snapshot before the
     * websocket connection at /ws/upload-jobs/{id} takes over with live
     * updates.
     */
    @GetMapping("/jobs/{id}")
    public ResponseEntity<?> getJob(@PathVariable UUID id) {
        return uploadJobRepository.findById(id)
                .map(UploadJobStatusDto::from)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "No upload job found with id " + id)));
    }
}