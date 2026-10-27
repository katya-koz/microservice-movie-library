package com.katyaflix.uploadservice.dto.metadata;

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
}