package com.katyaflix.userservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * One "My List" entry. Exactly one of movieId/showId is set — mirrored by
 * the DB check constraint, and by {@link #prePersist()} below as a
 * belt-and-braces check at the application layer.
 */
@Entity
@Table(name = "watchlists")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Watchlist {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private UUID movieId;

    private UUID showId;

    private OffsetDateTime added;

    @PrePersist
    void prePersist() {
        if (added == null) {
            added = OffsetDateTime.now();
        }
        boolean hasMovie = movieId != null;
        boolean hasShow = showId != null;
        if (hasMovie == hasShow) {
            throw new IllegalStateException("Exactly one of movieId or showId must be set");
        }
    }
}
