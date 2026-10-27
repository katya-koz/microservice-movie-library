package com.katyaflix.catalogservice.dto.metadata;

import com.katyaflix.catalogservice.entity.Season;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SeasonMetadata(
        Long tmdbId,
        UUID catalogId,
        Integer seasonNumber,
        String title,
        String overview,
        LocalDate airDate,
        String posterPath,
        List<EpisodeMetadata> episodes
) {

    public SeasonMetadata(Season season) {
        this(
                season.getTmdbId(),
                season.getId(),
                season.getSeasonNumber(),
                season.getTitle(),
                season.getOverview(),
                season.getAirDate(),
                season.getPosterPath(),
                season.getEpisodes().stream().map(e -> new EpisodeMetadata(e)).toList()
        );
    }
}