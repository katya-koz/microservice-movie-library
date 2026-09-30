package com.katyaflix.userservice.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record WatchtimeDto(
        MediaType mediaType,
        UUID mediaId,
        long watchtimeSeconds,
        OffsetDateTime updatedAt,
        long durationSeconds
) {
}
