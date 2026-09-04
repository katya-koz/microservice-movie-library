package com.katyaflix.catalogservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "seasons", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"show_id", "season_number"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Season implements Persistable<UUID> {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Transient
    private boolean isNew = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "show_id", nullable = false)
    private Show show;

    // 0 = specials, matches TMDB's convention
    @Column(name = "season_number", nullable = false)
    private Integer seasonNumber;

    @Column(name = "tmdb_id")
    private long tmdbId;

    private String title;

    @Column(columnDefinition = "text")
    private String overview;

    @Column(name = "poster_path")
    private String posterPath;

    @Column(name = "tmdb_poster_path")
    private String tmdbPosterPath;

    @Column(name = "air_date")
    private LocalDate airDate;

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


    @OneToMany(mappedBy = "season", fetch = FetchType.LAZY, cascade = CascadeType.ALL,  orphanRemoval = true )
    @Builder.Default
    private List<Episode> episodes = new ArrayList<>();
}