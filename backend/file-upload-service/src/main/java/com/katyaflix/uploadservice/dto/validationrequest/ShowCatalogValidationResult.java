package com.katyaflix.uploadservice.dto.validationrequest;

import com.katyaflix.uploadservice.dto.enums.EventStatus;
import com.katyaflix.uploadservice.dto.metadata.ShowMetadata;

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
