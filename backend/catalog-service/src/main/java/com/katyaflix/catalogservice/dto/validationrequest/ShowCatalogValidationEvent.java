package com.katyaflix.catalogservice.dto.validationrequest;

import com.katyaflix.catalogservice.dto.metadata.ShowMetadata;

import java.time.Instant;
import java.util.UUID;

public record ShowCatalogValidationEvent(
        UUID uploadJobId,
        Instant instant,
        ShowMetadata metadata
) {}
