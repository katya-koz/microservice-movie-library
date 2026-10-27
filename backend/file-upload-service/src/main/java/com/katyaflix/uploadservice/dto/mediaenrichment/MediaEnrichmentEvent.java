package com.katyaflix.uploadservice.dto.mediaenrichment;

import com.katyaflix.uploadservice.dto.enums.MediaCategory;

import java.time.Instant;
import java.util.UUID;

public record MediaEnrichmentEvent (
        UUID fileUploadId,
        Instant timestamp,
        MediaCategory mediaType,
        MediaEnrichment mediaFile
){
}
