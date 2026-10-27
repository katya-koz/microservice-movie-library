package com.katyaflix.uploadservice.dto.metadata;

import com.katyaflix.uploadservice.dto.enums.MediaCategory;

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
}

