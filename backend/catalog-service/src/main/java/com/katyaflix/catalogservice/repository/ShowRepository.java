package com.katyaflix.catalogservice.repository;

import com.katyaflix.catalogservice.entity.Movie;
import com.katyaflix.catalogservice.entity.Show;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShowRepository extends JpaRepository<Show, UUID> {

    interface ShowCatalogProjection {
        UUID getId();
        String getTitle();
        String getPosterPath();

    }

    Page<ShowCatalogProjection> findAllProjectedBy(Pageable pageable);
    Page<ShowCatalogProjection> findByTitleContainingIgnoreCase(String title, Pageable pageable);
    Optional<Show> findByTmdbId(long tmdbId);
    List<ShowCatalogProjection> findByIdIn(List<UUID> ids);


    @Modifying
    @Query("""
    UPDATE Show e
    SET e.posterPath = :posterPath
    WHERE e.id = :id
    """)
    int updatePosterPath(
            @Param("id") UUID id,
            @Param("posterPath") String posterPath
    );


    @Modifying
    @Query("""
    UPDATE Show e
    SET e.backdropPath = :backdropPath
    WHERE e.id = :id
    """)
    int updateBackdropPath(
            @Param("id") UUID id,
            @Param("backdropPath") String backdropPath
    );
}