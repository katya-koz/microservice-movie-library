package com.katyaflix.userservice.messaging.event;

import com.katyaflix.userservice.dto.MediaType;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Published to app.kafka.topics.watchtime-events on every watchtime upsert. */
public record WatchtimeUpdatedEvent(
        UUID userId,
        com.katyaflix.userservice.dto.@jakarta.validation.constraints.NotNull MediaType mediaType,
        UUID mediaId,
        long watchtimeSeconds,
        OffsetDateTime updatedAt
) {
}
