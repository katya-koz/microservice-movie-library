package com.katyaflix.catalogservice.dto.mediaenrichment;

import com.katyaflix.catalogservice.dto.enums.EventStatus;
import com.katyaflix.catalogservice.dto.metadata.MovieMetadata;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record  MediaEnrichmentResult(
//        MediaEnrichment enrichment,
        UUID uploadJobId,
        Instant completedAt,
        EventStatus status,
        String message

) {
}

