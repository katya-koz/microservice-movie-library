package com.katyaflix.catalogservice.controller;

import com.katyaflix.catalogservice.dto.CatalogDtos;
import com.katyaflix.catalogservice.dto.CatalogDtos.EpisodeDetail;
import com.katyaflix.catalogservice.dto.CatalogDtos.ShowDetail;
import com.katyaflix.catalogservice.dto.CatalogDtos.ShowSummary;
import com.katyaflix.catalogservice.dto.EpisodeDto;
import com.katyaflix.catalogservice.service.ShowService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/shows")
@RequiredArgsConstructor
public class ShowController {

    private final ShowService showService;

    // GET /api/shows?page=0&size=24&sort=title,asc
    @GetMapping
    public Page<ShowSummary> getShows(@RequestParam(required = false) String title, @PageableDefault(size = 24, sort = "title") Pageable pageable) {
        return showService.getCatalogPage(title, pageable);
    }

    // GET /shows/summary?ids=uuid1,uuid2,uuid3
    @GetMapping("/summary")
    public List<CatalogDtos.ShowSummary> getSummaries(@RequestParam List<UUID> ids) {
        return showService.getSummaries(ids);
    }

    // GET /api/shows/{id} -includes season summaries for the dropdown
    // not full episode lists (thats the endpoint below)
    @GetMapping("/{id}")
    public ShowDetail getShow(@PathVariable UUID id) {
        return showService.getDetail(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Show not found"));
    }

    // GET /api/shows/{showId}/seasons/{seasonNumber}/episodes
    @GetMapping("/{showId}/seasons/{seasonNumber}/episodes")
    public List<EpisodeDetail> getEpisodes(
            @PathVariable UUID showId,
            @PathVariable Integer seasonNumber
    ) {
        return showService.getEpisodesForSeason(showId, seasonNumber);
    }


    // GET /api/shows/{showId}/first-episode
    @GetMapping("/{showId}/first-episode")
    public EpisodeDto getFirstEpisode(
            @PathVariable UUID showId
    ) {
        return showService.getFirstEpisode(showId);
    }
}