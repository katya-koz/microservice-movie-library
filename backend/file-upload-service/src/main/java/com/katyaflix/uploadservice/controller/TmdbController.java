package com.katyaflix.uploadservice.controller;

import com.katyaflix.uploadservice.service.TmdbRequestException;
import com.katyaflix.uploadservice.service.TmdbService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/tmdb")
public class TmdbController {

    private final TmdbService tmdbService;

    public TmdbController(TmdbService tmdbService) {
        this.tmdbService = tmdbService;
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam String type, @RequestParam(required = false) String query) {
        try {
            return ResponseEntity.ok(tmdbService.search(type, query));
        } catch (TmdbRequestException e) {
            return errorResponse(e);
        }
    }

    @GetMapping("/details")
    public ResponseEntity<?> details(@RequestParam String type, @RequestParam long id) {
        try {
            return ResponseEntity.ok(tmdbService.details(type, id));
        } catch (TmdbRequestException e) {
            return errorResponse(e);
        }
    }

    @GetMapping("/season")
    public ResponseEntity<?> season(@RequestParam long tvId, @RequestParam int season) {
        try {
            return ResponseEntity.ok(tmdbService.season(tvId, season));
        } catch (TmdbRequestException e) {
            return errorResponse(e);
        }
    }

    private ResponseEntity<?> errorResponse(TmdbRequestException e) {
        HttpStatus status = HttpStatus.resolve(e.getStatus());
        if (status == null) status = HttpStatus.BAD_GATEWAY;
        return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
    }
}
