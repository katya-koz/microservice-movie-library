package com.katyaflix.catalogservice.dto.mediaenrichment;

import com.katyaflix.catalogservice.dto.enums.MediaCategory;

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
