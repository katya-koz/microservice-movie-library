package com.katyaflix.catalogservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "movies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Movie implements Persistable<UUID> {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "tmdb_id", unique = true)
    private long tmdbId;

    @Column(name = "title", nullable = false)
    private String title;

    @Transient
    private boolean isNew = true;

//    @Column(name = "original_title")
//    private String originalTitle;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(columnDefinition = "text")
    private String overview;

    // Comma-separated creator/director names
    @Column(name = "creator_names")
    private String creatorNames;

    // Local, self-hosted copy — this is what the app actually serves
    @Column(name = "poster_path")
    private String posterPath;

    @Column(name = "backdrop_path")
    private String backdropPath;

    // Raw TMDB fragment, kept for re-download/debugging — never served directly
    @Column(name = "tmdb_poster_path")
    private String tmdbPosterPath;

    @Column(name = "tmdb_backdrop_path")
    private String tmdbBackdropPath;

    @Column(name = "runtime_minutes")
    private Integer runtimeMinutes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
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

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}