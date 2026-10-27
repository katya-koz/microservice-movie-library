package com.katyaflix.uploadservice.dto.validationrequest;

import com.katyaflix.uploadservice.dto.metadata.ShowMetadata;

import java.time.Instant;
import java.util.UUID;

public record ShowCatalogValidationEvent(
        UUID uploadJobId,
        Instant instant,
        ShowMetadata metadata
) {}
