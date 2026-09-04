package com.katyaflix.catalogservice.controller;

import com.katyaflix.catalogservice.dto.CatalogDtos.NextEpisode;
import com.katyaflix.catalogservice.dto.CatalogDtos.PlaybackInfo;
import com.katyaflix.catalogservice.service.PlaybackService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/playback")
@RequiredArgsConstructor
public class PlaybackController {

    private final PlaybackService playbackService;

    // GET /api/playback/movies/{movieId}
    @GetMapping("/movies/{movieId}")
    public PlaybackInfo getMoviePlayback(@PathVariable UUID movieId) {
        return playbackService.resolveForMovie(movieId);
    }

    // GET /api/playback/episodes/{episodeId}
    @GetMapping("/episodes/{episodeId}")
    public PlaybackInfo getEpisodePlayback(@PathVariable UUID episodeId) {
        return playbackService.resolveForEpisode(episodeId);
    }

    // GET /api/playback/episodes/{episodeId}/next
    // for autoplay —> 404 if this was the last episode of the show
    @GetMapping("/episodes/{episodeId}/next")
    public NextEpisode getNextEpisode(@PathVariable UUID episodeId) {
        return playbackService.getNext(episodeId);
    }
}