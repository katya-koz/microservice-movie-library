package com.katyaflix.userservice.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record WatchlistItemDto(
        UUID id,
        MediaType mediaType,
        UUID mediaId,
        OffsetDateTime added
) {
}
