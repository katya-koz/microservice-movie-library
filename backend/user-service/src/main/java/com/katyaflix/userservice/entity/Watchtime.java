package com.katyaflix.userservice.entity;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Playback position for a single user + movie/episode. Exactly one of
 * movieId/episodeId is set, same pattern as {@link Watchlist}.
 */
@Entity
@Table(name = "watchtimes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Watchtime {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // we dont need a fk relationship... just the keys are fine
    private UUID movieId;

    private UUID episodeId;
    private UUID showId;
    private UUID seasonId;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    private long watchtimeSeconds;
    private long durationSeconds;

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
        boolean hasMovie = movieId != null;
        boolean hasEpisode = episodeId != null;
        if (hasMovie == hasEpisode) {
            throw new IllegalStateException("Exactly one of movieId or episodeId must be set");
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

}
