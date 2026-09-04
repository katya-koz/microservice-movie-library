package com.katyaflix.catalogservice.repository;

import com.katyaflix.catalogservice.entity.Episode;

import com.katyaflix.catalogservice.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EpisodeRepository extends JpaRepository<Episode, UUID> {

    List<Episode> findBySeasonIdOrderByEpisodeNumberAsc(UUID seasonId);

    long countBySeasonId(UUID seasonId);

    Optional<Episode> findFirstBySeasonIdAndEpisodeNumberGreaterThanOrderByEpisodeNumberAsc(
            UUID seasonId, Integer episodeNumber
    );

    Optional<Episode> findFirstBySeasonIdOrderByEpisodeNumberAsc(UUID seasonId);
    Optional<Episode> findByTmdbId(long tmdbId);

    @Query("""
        select e from Episode e
        join fetch e.season s
        join fetch s.show sh
        where e.id = :id
    """)
    Optional<Episode> findByIdWithSeasonAndShow(@Param("id") UUID id);
    Optional<Episode> findBySeasonIdAndEpisodeNumber(UUID seasonId, Integer episodeNumber);
    List<Episode> findByIdIn(List<UUID> ids);


    @Modifying
    @Query("""
    UPDATE Episode e
    SET e.stillPath = :stillPath
    WHERE e.id = :id
    """)
    int updateStillPath(
            @Param("id") UUID id,
            @Param("stillPath") String stillPath
    );
}