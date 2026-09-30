package com.katyaflix.userservice.controller;
import com.katyaflix.userservice.dto.AddToWatchlistRequest;
import com.katyaflix.userservice.dto.MediaType;
import com.katyaflix.userservice.dto.WatchlistItemDto;
import com.katyaflix.userservice.service.WatchlistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** "My List" — add/remove movies & shows, and list what's on it. */
@RestController
@RequestMapping("/profiles/{userId}/watchlist")
@RequiredArgsConstructor
public class WatchlistController {

    private final WatchlistService watchlistService;

    @GetMapping
    public List<WatchlistItemDto> getWatchlist(@PathVariable UUID userId) {
        return watchlistService.getWatchlist(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WatchlistItemDto addToWatchlist(@PathVariable UUID userId, @Valid @RequestBody AddToWatchlistRequest request) {
        return watchlistService.addToWatchlist(userId, request);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFromWatchlist(@PathVariable UUID userId, @RequestParam MediaType mediaType, @RequestParam UUID mediaId) {
        watchlistService.removeFromWatchlist(userId, mediaType, mediaId);
    }
}
