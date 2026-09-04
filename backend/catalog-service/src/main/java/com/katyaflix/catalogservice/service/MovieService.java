package com.katyaflix.catalogservice.service;

import com.katyaflix.catalogservice.dto.CatalogDtos.MovieDetail;
import com.katyaflix.catalogservice.dto.CatalogDtos.MovieSummary;
import com.katyaflix.catalogservice.entity.Movie;
import com.katyaflix.catalogservice.repository.MediaFileRepository;
import com.katyaflix.catalogservice.repository.MovieRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;
    private final MediaFileRepository mediaFileRepository;

    public Page<MovieSummary> getCatalogPage(Pageable pageable) {
        return movieRepository.findAllProjectedBy(pageable).map(p -> new MovieSummary(p.getId(), p.getTitle(), p.getPosterPath()));
    }

    public Optional<MovieDetail> getDetail(UUID movieId) {
        return movieRepository.findById(movieId).map(this::toDetail);
    }

    public List<MovieSummary> getSummaries(List<UUID> ids) {
        return movieRepository.findByIdIn(ids).stream().map(p -> new MovieSummary(p.getId(), p.getTitle(), p.getPosterPath())).toList();
    }

    private MovieDetail toDetail(Movie m) {
        return new MovieDetail(
                m.getId(),
                m.getTitle(),
                m.getOverview(),
                m.getRuntimeMinutes(),
                m.getReleaseDate(),
                m.getCreatorNames(),
                m.getPosterPath(),
                m.getBackdropPath()
        );
    }


}