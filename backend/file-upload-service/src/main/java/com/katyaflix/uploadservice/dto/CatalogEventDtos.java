package com.katyaflix.uploadservice.dto;


import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public class CatalogEventDtos {

    public enum CatalogUpdateStatus {
        FAILURE,
        SUCCESS // upsert? insert ?? maybe...
    }


    // dto to pass over tmdb information + file location information to catalog the new uploads
    public record MovieCatalogValidationEvent
            (UUID fileUploadId, // reference to upload job in file upload table
             Instant instant,
             FileUploadMetadata.MovieUploadMetadataDto metadata
            )
    {
    }

    public record MediaCatalogEnrichmentEvent(
            UUID fileUploadId,
            Instant instant,
            CatalogUpdateStatus status,
            String message
    ) {}

    public record FilePathUpdateEvent(
            UUID fileUploadId,
            Instant instant,
            CatalogUpdateStatus status,
            String message
    ) {}




    public record ShowCatalogValidationEvent(
            UUID fileUploadId,
            Instant instant,
            FileUploadMetadata.ShowUploadMetadataDto metadata
    ) {}



    // these are sent from catalog -> upload service
    public record CatalogMovieUploadEvent (
            FileUploadMetadata.MovieUploadMetadataDto metadata,
            CatalogUpdateStatus status,
            String message,
            LocalDate completedAt,
            UUID fileUploadId
    )
    { }


    public record CatalogShowUploadEvent (
            FileUploadMetadata.ShowUploadMetadataDto metadata,
            CatalogUpdateStatus status,
            String message,
            LocalDate completedAt,
            UUID fileUploadId
    )
    {  }

    public record MediaFileEnrichmentEvent(
            UUID fileUploadId,
            Instant timestamp,
            MediaFileEnrichmentDto mediaFile
    ) {}

    public record MediaFileEnrichmentDto(
            UUID movieId,
            UUID episodeId,
            String originalFilename,
            String containerFormat,
            String videoCodec,
            String audioCodec,
            String resolution,
            Integer durationSeconds,
            Long fileSizeBytes,
            Instant encodedAt
    ) {}



    /// path updates:

    public record CatalogPathUpdateEvent
            (UUID fileUploadId,
             Instant timestamp,
             CatalogPathUpdateDto pathUpdates)
    {}
    public record CatalogPathUpdateDto (
            Map<UUID, String> mediaFilePaths,
            Map<UUID, String> episodeStillPaths,
            Map<UUID, String> seasonPosterPaths,
            Map<UUID, String> showPosterPaths,
            Map<UUID, String> showBackdropPaths,
            Map<UUID, String> moviePosterPaths,
            Map<UUID, String> movieBackdropPaths
    ){}

}
