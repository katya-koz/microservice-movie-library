package com.katyaflix.uploadservice.dto.metadata;

import com.katyaflix.uploadservice.dto.enums.MediaCategory;

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
}