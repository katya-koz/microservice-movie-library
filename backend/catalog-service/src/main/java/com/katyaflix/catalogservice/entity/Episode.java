package com.katyaflix.catalogservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "episodes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"season_id", "episode_number"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Episode implements Persistable<UUID> {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Transient
    private boolean isNew = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    @Column(name = "episode_number", nullable = false)
    private Integer episodeNumber;

    @Column(name = "tmdb_id")
    private long tmdbId;

    @Column(name = "title")
    private String title;

    @Column(columnDefinition = "text")
    private String overview;

    @Column(name = "air_date")
    private LocalDate airDate;

    @Column(name = "runtime_minutes")
    private Integer runtimeMinutes;

    @Column(name = "still_path")
    private String stillPath;

    @Column(name = "tmdb_still_path")
    private String tmdbStillPath;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @jakarta.persistence.PostLoad
    @jakarta.persistence.PostPersist
    void markNotNew() {
        this.isNew = false;
    }
}