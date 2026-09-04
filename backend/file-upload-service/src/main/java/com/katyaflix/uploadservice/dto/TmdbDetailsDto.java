package com.katyaflix.uploadservice.dto;

import java.util.List;

public record TmdbDetailsDto(
        List<String> genres,
        Integer runtime,
        String backdropPath,
        String overview,
        List<String> creators, // directors (movie) or created_by (tv)
        String status // tv only (ended, running, etc)
) {}
