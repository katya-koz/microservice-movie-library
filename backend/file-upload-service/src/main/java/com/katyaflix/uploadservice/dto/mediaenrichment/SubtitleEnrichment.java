package com.katyaflix.uploadservice.dto.mediaenrichment;

import java.time.Instant;
import java.util.UUID;

public record SubtitleEnrichment
        (
             UUID id,
             String originalFilename,
             String filePath ,
             String languageCode,
             String label,
             String format,
             Instant encodedAt
        ) {}
