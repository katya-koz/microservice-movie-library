package com.katyaflix.catalogservice.dto.validationrequest;


import com.katyaflix.catalogservice.dto.metadata.MovieMetadata;

import java.time.Instant;
import java.util.UUID;

public record MovieCatalogValidationEvent(
        UUID uploadJobId, // reference to upload job in file upload table
        Instant instant,
        MovieMetadata metadata
)
{
}