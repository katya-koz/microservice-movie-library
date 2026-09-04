package com.katyaflix.uploadservice;

import com.katyaflix.uploadservice.dto.FileUploadMetadata;
import com.katyaflix.uploadservice.messaging.CatalogValidationProducer;
import com.katyaflix.uploadservice.service.TmdbService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.katyaflix.uploadservice.entity.UploadJob;
import com.katyaflix.uploadservice.service.UploadProcessingException;
import com.katyaflix.uploadservice.service.UploadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.util.Map;

@RestController
@RequestMapping("/uploads")
public class UploadController {

    private final ObjectMapper objectMapper;
    private final UploadService uploadService;
    private final TmdbService tmdbService;
    private final CatalogValidationProducer catalogValidationProducer;

    public UploadController(
            ObjectMapper objectMapper,
            UploadService uploadService,
            TmdbService tmdbService,
            CatalogValidationProducer catalogValidationProducer
            )
    {
        this.objectMapper = objectMapper;
        this.uploadService = uploadService;
        this.tmdbService = tmdbService;
        this.catalogValidationProducer = catalogValidationProducer;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(
            @RequestParam("metadata") String metadataJson,
            MultipartHttpServletRequest request
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

        UploadJob job = uploadService.createJob(metadata);


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
}
