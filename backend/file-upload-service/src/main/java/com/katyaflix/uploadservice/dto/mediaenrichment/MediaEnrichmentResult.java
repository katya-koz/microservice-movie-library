package com.katyaflix.uploadservice.dto.mediaenrichment;

import com.katyaflix.uploadservice.dto.enums.EventStatus;

import java.time.Instant;
import java.util.UUID;

public record  MediaEnrichmentResult(
//        MediaEnrichment enrichment,
        UUID fileUploadId,
        Instant completedAt,
        EventStatus status,
        String message

) {
}

