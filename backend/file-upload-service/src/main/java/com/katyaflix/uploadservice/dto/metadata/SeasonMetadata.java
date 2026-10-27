package com.katyaflix.uploadservice.dto.metadata;

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
}