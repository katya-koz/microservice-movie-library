package com.katyaflix.catalogservice.controller;

import com.katyaflix.catalogservice.dto.CatalogDtos.MovieDetail;
import com.katyaflix.catalogservice.dto.CatalogDtos.MovieSummary;
import com.katyaflix.catalogservice.service.MovieService;

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
@RequestMapping("/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;

    // GET /movies?page=0&size=24&sort=title,asc
    @GetMapping
    public Page<MovieSummary> getMovies(@RequestParam(required = false) String title,@PageableDefault(size = 24, sort = "title") Pageable pageable) {
        return movieService.getCatalogPage(title, pageable);
    }
    // GET /movies/summary?ids=uuid1,uuid2,uuid3
    @GetMapping("/summary")
    public List<MovieSummary> getSummaries(@RequestParam List<UUID> ids) {
        return movieService.getSummaries(ids);
    }

    // GET /movies/{id}
    @GetMapping("/{id}")
    public MovieDetail getMovie(@PathVariable UUID id) {
        return movieService.getDetail(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Movie not found"));
    }


}