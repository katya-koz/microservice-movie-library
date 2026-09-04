package com.katyaflix.uploadservice.dto;

public record TmdbSeasonEpisodeDto(
        int episodeNumber,
        String name,
        String overview,
        String stillPath
) {}
