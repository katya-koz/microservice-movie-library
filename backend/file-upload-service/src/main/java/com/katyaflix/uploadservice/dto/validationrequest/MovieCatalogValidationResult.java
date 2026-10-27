package com.katyaflix.uploadservice.dto.validationrequest;


import com.katyaflix.uploadservice.dto.enums.EventStatus;
import com.katyaflix.uploadservice.dto.metadata.MovieMetadata;

import java.time.LocalDate;
import java.util.UUID;

// catalog validation results from catalog -> file upload
public record MovieCatalogValidationResult (
        MovieMetadata metadata,

        EventStatus status,
        String message,
        LocalDate completedAt,
        UUID fileUploadId
)
{ }
