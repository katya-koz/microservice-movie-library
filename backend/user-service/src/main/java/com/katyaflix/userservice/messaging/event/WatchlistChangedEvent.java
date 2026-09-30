package com.katyaflix.userservice.messaging.event;

import com.katyaflix.userservice.dto.MediaType;

import java.util.UUID;

/** Published to app.kafka.topics.watchlist-events on add/remove. */
public record WatchlistChangedEvent(
        UUID userId,
        MediaType mediaType,
        UUID mediaId,
        ChangeType changeType
) {
    public enum ChangeType {
        ADDED,
        REMOVED
    }
}
