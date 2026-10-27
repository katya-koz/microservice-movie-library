package com.katyaflix.catalogservice.repository;

import com.katyaflix.catalogservice.entity.Subtitle;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubtitleRepository extends JpaRepository<Subtitle, UUID> {
    List<Subtitle> findByMediaFileId(UUID mediaFileId);

    // filePath is unique - used to make subtitle upserts idempotent in case
    // a CatalogPathUpdateEvent is ever redelivered (Kafka is at-least-once)
    Optional<Subtitle> findByFilePath(String filePath);

    Optional<Subtitle> findByOriginalFileName(String originalName);
//    void deleteByMediaFileMovieId(UUID movieId);
//    void deleteByMediaFileEpisodeId(UUID episodeId);

    Optional<Subtitle> findByMovieIdAndOriginalFileName(UUID movieId, String originalFileName);
    Optional<Subtitle> findByEpisodeIdAndOriginalFileName(UUID episodeId, String originalFileName);

    // for PlaybackService (see below)
    List<Subtitle> findByMovieId(UUID movieId);
    List<Subtitle> findByEpisodeId(UUID episodeId);
}