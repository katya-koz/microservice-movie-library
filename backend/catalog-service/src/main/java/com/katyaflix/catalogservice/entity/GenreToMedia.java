package com.katyaflix.catalogservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table( name = "genre_to_media", uniqueConstraints = { @UniqueConstraint( name = "uq_genre_to_media_genre_movie", columnNames = {"genre_id", "movie_id"} ),
        @UniqueConstraint( name = "uq_genre_to_media_genre_show", columnNames = {"genre_id", "show_id"} ) })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenreToMedia {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "genre_id", nullable = false)
    private Genre genre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id")
    private Movie movie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "show_id")
    private Show show;

    @PrePersist
    @PreUpdate
    private void validateMedia() {
        if ((movie == null) == (show == null)) {
            throw new IllegalStateException(
                    "GenreToMedia must reference exactly one of movie or show"
            );
        }
    }
}