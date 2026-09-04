package com.katyaflix.catalogservice.repository;

import com.katyaflix.catalogservice.entity.Subtitle;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SubtitleRepository extends JpaRepository<Subtitle, UUID> {
    List<Subtitle> findByMediaFileId(UUID mediaFileId);
//    void deleteByMediaFileMovieId(UUID movieId);
//    void deleteByMediaFileEpisodeId(UUID episodeId);
}