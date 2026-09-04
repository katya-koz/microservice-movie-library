package com.katyaflix.uploadservice.dto;

public record TmdbSearchResultDto(
        long id,
        String title,
        String year,
        String releaseDate, // full ISO date if TMDB has one, else null
        String posterPath,
        String overview
) {}


