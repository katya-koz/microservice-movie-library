package com.katyaflix.catalogservice.dto.validationrequest;

import com.katyaflix.catalogservice.dto.enums.EventStatus;
import com.katyaflix.catalogservice.dto.metadata.ShowMetadata;

import java.time.LocalDate;
import java.util.UUID;

public record ShowCatalogValidationResult (
        ShowMetadata metadata,
        EventStatus status,
        String message,
        LocalDate completedAt,
        UUID fileUploadId
)
{  }
