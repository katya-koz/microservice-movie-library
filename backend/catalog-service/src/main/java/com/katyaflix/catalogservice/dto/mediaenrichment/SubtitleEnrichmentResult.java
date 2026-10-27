package com.katyaflix.catalogservice.dto.mediaenrichment;

import com.katyaflix.catalogservice.dto.enums.EventStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SubtitleEnrichmentResult
    (
//            SubtitleEnrichment enrichment,
            UUID uploadJobId,
            Instant completedAt,
            EventStatus status,
            String message
    ) {
    }




