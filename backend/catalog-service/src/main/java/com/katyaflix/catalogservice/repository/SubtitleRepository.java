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
//    void deleteByMediaFileMovieId(UUID movieId);
//    void deleteByMediaFileEpisodeId(UUID episodeId);
}