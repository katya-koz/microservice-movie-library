package com.katyaflix.catalogservice.dto.metadata;

import com.katyaflix.catalogservice.entity.Episode;

import java.time.LocalDate;
import java.util.UUID;

public record EpisodeMetadata(
        Long tmdbId,
        UUID catalogId,
        String title,
        LocalDate airDate,
        String overview,
        String creatorNames,
        Integer runtimeMinutes,
        Integer episodeNumber,
        String stillPath
) {
    public EpisodeMetadata(Episode episode) {
        this(
                episode.getTmdbId(),
                episode.getId(),
                episode.getTitle(),
                episode.getAirDate(),
                episode.getOverview(),
                "",
                episode.getRuntimeMinutes(),
                episode.getEpisodeNumber(),
                episode.getStillPath()
        );
    }

}