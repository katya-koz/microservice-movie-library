package com.katyaflix.userservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpdateWatchtimeRequest(
        @NotNull MediaType mediaType,
        @NotNull UUID mediaId,
        @Min(0) long watchtimeSeconds,
        @Min(0) long durationSeconds,
        UUID showId,
        UUID seasonId
) {
}
