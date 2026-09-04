package com.katyaflix.catalogservice.repository;

import com.katyaflix.catalogservice.entity.Movie;
import com.katyaflix.catalogservice.entity.Season;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SeasonRepository extends JpaRepository<Season, UUID> {

    List<Season> findByShowIdOrderBySeasonNumberAsc(UUID showId);

    Optional<Season> findByShowIdAndSeasonNumber(UUID showId, Integer seasonNumber);

    Optional<Season> findFirstByShowIdAndSeasonNumberGreaterThanOrderBySeasonNumberAsc(
            UUID showId, Integer seasonNumber
    );

    Optional<Season> findByTmdbId(long tmdbId);



    @Modifying
    @Query("""
    UPDATE Season e
    SET e.posterPath = :posterPath
    WHERE e.id = :id
    """)
    int updatePosterPath(
            @Param("id") UUID id,
            @Param("posterPath") String posterPath
    );


 
}