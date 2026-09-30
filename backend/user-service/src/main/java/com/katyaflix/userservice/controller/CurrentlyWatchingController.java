package com.katyaflix.userservice.controller;

import com.katyaflix.userservice.dto.EpisodeDto;
import com.katyaflix.userservice.dto.MediaDto;
import com.katyaflix.userservice.service.CurrentlyWatchingService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/profiles/{userId}/currently-watching")
@RequiredArgsConstructor
public class CurrentlyWatchingController {

    private final CurrentlyWatchingService currentlyWatchingService;

    @GetMapping()
    public List<MediaDto> getCurrentlyWatching(@PathVariable UUID userId) {
        return currentlyWatchingService.getCurrentlyWatching(userId);
    }


    @GetMapping("/{showId}")
    public EpisodeDto getCurrentlyWatchingEpisode(@PathVariable UUID userId, @PathVariable UUID showId) {
        return currentlyWatchingService.getCurrentEpisode(userId, showId);
    }
}