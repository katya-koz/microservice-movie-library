package com.katyaflix.userservice.controller;

import com.katyaflix.userservice.dto.MediaType;
import com.katyaflix.userservice.dto.UpdateWatchtimeRequest;
import com.katyaflix.userservice.dto.WatchtimeDto;
import com.katyaflix.userservice.service.WatchtimeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST fallback/initial-load path for watchtimes. The player uses GET
 * .../movie/{id} or .../episode/{id} once, on load, to know where to seek
 * to; ongoing updates while playing go over the WebSocket instead (see
 * websocket.WatchtimeWebSocketHandler) but PUT is here too for clients that
 * can't hold a socket open (or for a final flush).
 */
@RestController
@RequestMapping("/profiles/{userId}/watchtimes")
@RequiredArgsConstructor
public class WatchtimeController {

    private final WatchtimeService watchtimeService;

    @GetMapping("/movie/{movieId}")
    public ResponseEntity<WatchtimeDto> getMovieWatchtime(@PathVariable UUID userId, @PathVariable UUID movieId) {
        return watchtimeService.getWatchtime(userId, MediaType.MOVIE, movieId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.ok(new WatchtimeDto(MediaType.MOVIE, movieId, 0, null, 0)));
    }

    @GetMapping("/episode/{episodeId}")
    public ResponseEntity<WatchtimeDto> getEpisodeWatchtime(@PathVariable UUID userId, @PathVariable UUID episodeId) {
        return watchtimeService.getWatchtime(userId, MediaType.EPISODE, episodeId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.ok(new WatchtimeDto(MediaType.EPISODE, episodeId, 0, null, 0)));
    }


    @GetMapping("/episodes")
    public ResponseEntity<List<WatchtimeDto>> getEpisodeWatchtimes( @PathVariable UUID userId, @RequestParam List<UUID> episodeIds ) {
        return ResponseEntity.ok(
                watchtimeService.getWatchtimes(userId,MediaType.EPISODE,episodeIds)
        );
    }

    @PutMapping
    public WatchtimeDto updateWatchtime(@PathVariable UUID userId, @Valid @RequestBody UpdateWatchtimeRequest request) {
        return watchtimeService.upsertWatchtime(userId, request);
    }


}
