package com.katyaflix.userservice.messaging.event;

import java.util.UUID;

/** Published to app.kafka.topics.profile-events on create/update/delete. */
public record ProfileEvent(
        UUID userId,
        String displayName,
        ChangeType changeType
) {
    public enum ChangeType {
        CREATED,
        UPDATED,
        DELETED
    }
}
