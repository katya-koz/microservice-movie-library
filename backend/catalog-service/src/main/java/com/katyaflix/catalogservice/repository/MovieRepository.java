package com.katyaflix.catalogservice.repository;

import com.katyaflix.catalogservice.entity.Movie;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MovieRepository extends JpaRepository<Movie, UUID> {

    interface MovieCatalogProjection {
        UUID getId();
        String getTitle();
        String getPosterPath();
    }

    Optional<Movie> findByTmdbId(long tmdbId);

    Page<MovieCatalogProjection> findAllProjectedBy(Pageable pageable);

    List<MovieCatalogProjection> findByIdIn(List<UUID> ids);


    @Modifying
    @Query("""
    UPDATE Movie e
    SET e.posterPath = :posterPath
    WHERE e.id = :id
    """)
    int updatePosterPath(
            @Param("id") UUID id,
            @Param("posterPath") String posterPath
    );


    @Modifying
    @Query("""
    UPDATE Movie e
    SET e.backdropPath = :backdropPath
    WHERE e.id = :id
    """)
    int updateBackdropPath(
            @Param("id") UUID id,
            @Param("backdropPath") String backdropPath
    );
}