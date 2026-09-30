package com.katyaflix.userservice.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddToWatchlistRequest(
        @NotNull MediaType mediaType,
        @NotNull UUID mediaId
) {
}
