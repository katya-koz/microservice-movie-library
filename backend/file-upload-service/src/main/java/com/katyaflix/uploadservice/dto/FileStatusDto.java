package com.katyaflix.uploadservice.dto;

import com.katyaflix.uploadservice.dto.enums.FileUploadStatus;
import com.katyaflix.uploadservice.entity.FileUpload;
import com.katyaflix.uploadservice.entity.PipelineState;

import java.util.UUID;

/**
 * One file's status. Used by GET /uploads/files/{id} and the websocket pushes, so the
 * frontend parses a single shape. Component names are the JSON field names (snake_case on purpose).
 */
public record FileStatusDto(
        UUID file_upload_id,
        UUID upload_job_id,
        String status,
        int progress_percent,
        int attempts,
        String error,
        String message,
        String final_path) {

    public static FileStatusDto from(FileUpload f) {
        // while the catalog is working, its stage holds the interesting message
        PipelineState stage = f.getStatus() == FileUploadStatus.AWAITING_ENRICHMENT
                ? f.getCatalogEnrichmentState()
                : f.getFileEncodeState();

        return new FileStatusDto(
                f.getId(),
                f.getUploadJobId(),
                String.valueOf(f.getStatus()),
                f.getProgressPercent(),
                f.getAttempts(),
                f.getError(),
                stage != null ? stage.getMessage() : null,
                f.getFinalPath());
    }
}