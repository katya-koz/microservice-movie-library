package com.katyaflix.uploadservice.dto;

import com.katyaflix.uploadservice.entity.UploadJob;

import java.time.Instant;
import java.util.List;
import java.util.UUID;


public record UploadJobStatusDto(
        UUID id,
        String mediaType,
        String title,
        UploadJob.UploadStatus status,
        String currentStep,
        Long tmdbId,
        boolean catalogValidationStatus,
        boolean fileUploadStatus,
        boolean mediaFileEnrichmentStatus,
        boolean filePathUpdateStatus,
        int encodedFileCount,
        List<String> errorMessages,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt
) {

    public static UploadJobStatusDto from(UploadJob job) {
        return new UploadJobStatusDto(
                job.getId(),
                job.getMediaType(),
                job.getTitle(),
                job.getStatus(),
                job.getCurrentStep(),
                job.getTmdbId(),
                job.getCatalogValidationStatus(),
                job.getFileUploadStatus(),
                job.getMediaFileEnrichmentStatus(),
                job.getFilePathUpdateStatus(),
                job.getEncodedFileCount(),
                job.getErrorMessages(),
                job.getCreatedAt(),
                job.getUpdatedAt(),
                job.getCompletedAt()
        );
    }
}
