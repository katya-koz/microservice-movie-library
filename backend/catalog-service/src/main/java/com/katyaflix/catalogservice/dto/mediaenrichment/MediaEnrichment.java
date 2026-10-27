package com.katyaflix.catalogservice.dto.mediaenrichment;

import com.katyaflix.catalogservice.dto.enums.MediaCategory;

import java.time.Instant;
import java.util.UUID;

public record MediaEnrichment
(    UUID id,
     String filePath ,
     String originalFilename,
     String containerFormat,
     String videoCodec,
     String audioCodec,
     String resolution,
     Integer durationSeconds,
     Long fileSizeBytes,
     Instant encodedAt

) {}
