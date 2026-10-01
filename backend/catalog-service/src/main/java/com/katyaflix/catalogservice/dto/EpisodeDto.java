package com.katyaflix.catalogservice.dto;

import java.util.UUID;

public record EpisodeDto(
        UUID episodeId,
        UUID seasonId,
        UUID showId,
        long watchedSeconds,
        long durationSeconds){

}
