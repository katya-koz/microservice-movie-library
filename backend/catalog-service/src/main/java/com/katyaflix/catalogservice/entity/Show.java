package com.katyaflix.catalogservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "shows")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Show implements Persistable<UUID> {

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

    @Column(name = "first_air_date")
    private LocalDate firstAirDate;

    @Column(columnDefinition = "text")
    private String overview;

    @Column(name = "creator_names")
    private String creatorNames;

    @Column(name = "poster_path")
    private String posterPath;

    @Column(name = "backdrop_path")
    private String backdropPath;

    @Column(name = "tmdb_poster_path")
    private String tmdbPosterPath;

    @Column(name = "tmdb_backdrop_path")
    private String tmdbBackdropPath;

    // Returning Series, Ended, Canceled, ...
    private String status;

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

    @OneToMany(mappedBy = "show", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Builder.Default
    private List<Season> seasons = new ArrayList<>();


    @OneToMany(mappedBy = "show",fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Builder.Default
    private List<GenreToMedia> genres = new ArrayList<>();
}