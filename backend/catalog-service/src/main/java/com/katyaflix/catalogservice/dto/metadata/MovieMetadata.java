package com.katyaflix.catalogservice.dto.metadata;


import com.katyaflix.catalogservice.dto.enums.MediaCategory;
import com.katyaflix.catalogservice.entity.Movie;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record MovieMetadata(
        long tmdbId,
        UUID catalogId,
        MediaCategory type,
        String title,
        LocalDate releaseDate,
        String overview,
        String creatorNames,
        Integer runtimeMinutes,
        String posterPath,
        String backdropPath,
        List<String> genres
) implements MediaMetadata {

    public MovieMetadata(Movie movie) {
        this(
                movie.getTmdbId(),
                movie.getId(),
                MediaCategory.MOVIE,
                movie.getTitle(),
                movie.getReleaseDate(),
                movie.getOverview(),
                movie.getCreatorNames(),
                movie.getRuntimeMinutes(),
                movie.getPosterPath(),
                movie.getBackdropPath(),
                movie.getGenres().stream().map(m -> m.getGenre().getName()).toList()
        );
    }
}

