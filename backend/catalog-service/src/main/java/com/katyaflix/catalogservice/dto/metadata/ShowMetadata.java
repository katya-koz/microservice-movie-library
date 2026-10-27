package com.katyaflix.catalogservice.dto.metadata;


import com.katyaflix.catalogservice.dto.enums.MediaCategory;
import com.katyaflix.catalogservice.entity.Show;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ShowMetadata(
        long tmdbId,
        UUID catalogId,
        MediaCategory type,
        String title,
        LocalDate firstAirDate,
        String overview,
        String creatorNames,
        String status,
        String posterPath,
        String backdropPath,
        List<String> genres,
        List<SeasonMetadata> seasons
) implements MediaMetadata {

    public ShowMetadata(Show show) {
        this(
                show.getTmdbId(),
                show.getId(),
                MediaCategory.SHOW,
                show.getTitle(),
                show.getFirstAirDate(),
                show.getOverview(),
                show.getCreatorNames(),
                show.getStatus(),
                show.getPosterPath(),
                show.getBackdropPath(),
                show.getGenres().stream().map(m -> m.getGenre().getName()).toList(),
                show.getSeasons().stream().map(s -> new SeasonMetadata(s)).toList()
        );
    }
}