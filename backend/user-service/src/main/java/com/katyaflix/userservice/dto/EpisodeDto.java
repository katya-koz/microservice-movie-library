package com.katyaflix.userservice.dto;

import java.util.UUID;

public record EpisodeDto (
        UUID episodeId,
        UUID seasonId,
        UUID showId,
        long watchedSeconds,
        long durationSeconds){

}
