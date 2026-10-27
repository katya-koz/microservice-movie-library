package com.katyaflix.catalogservice.service;

import com.katyaflix.catalogservice.dto.CatalogDtos.MovieDetail;
import com.katyaflix.catalogservice.dto.CatalogDtos.MovieSummary;
import com.katyaflix.catalogservice.dto.metadata.MovieMetadata;
import com.katyaflix.catalogservice.entity.Genre;
import com.katyaflix.catalogservice.entity.GenreToMedia;
import com.katyaflix.catalogservice.entity.Movie;
import com.katyaflix.catalogservice.repository.*;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MovieService {

    @Value("${media.url}")
    private String mediaUrlRoot;
    private final MovieRepository movieRepository;
    private final MediaFileRepository mediaFileRepository;
    private final GenreService genreService;

    public Page<MovieSummary> getCatalogPage(String title, Pageable pageable) {
        Page<MovieRepository.MovieCatalogProjection> movies;

        if (title == null || title.isBlank()) {
            movies = movieRepository.findAllProjectedBy(pageable);
        } else {
            movies = movieRepository.findByTitleContainingIgnoreCase(title, pageable);
        }

        return movies.map(p ->  new MovieSummary( p.getId(), p.getTitle(), mediaUrlRoot + p.getPosterPath() ) );
    }


    public Optional<MovieDetail> getDetail(UUID movieId) {
        return movieRepository.findById(movieId).map(this::toDetail);
    }

    public List<MovieSummary> getSummaries(List<UUID> ids) {
        return movieRepository.findByIdIn(ids).stream().map(p -> new MovieSummary(p.getId(), p.getTitle(), mediaUrlRoot + p.getPosterPath())).toList();
    }

    private MovieDetail toDetail(Movie m) {
        return new MovieDetail(
                m.getId(),
                m.getTitle(),
                m.getOverview(),
                m.getRuntimeMinutes(),
                m.getReleaseDate(),
                m.getCreatorNames(),
                mediaUrlRoot + m.getPosterPath(),
                mediaUrlRoot + m.getBackdropPath(),
                m.getGenres().stream().map(GenreToMedia::getGenre).map(Genre::getName).toList()
        );
    }




    // ********* UPSERT / ENRICHMENT

    @Transactional
    public Movie upsertMovie(MovieMetadata metadata) {
        Movie movie = findOrCreateByTmdbId(metadata.tmdbId());
        movie.setTmdbId(metadata.tmdbId());
        movie.setTitle(metadata.title());
        movie.setReleaseDate(metadata.releaseDate());
        movie.setOverview(metadata.overview());
        movie.setCreatorNames(metadata.creatorNames());
        movie.setRuntimeMinutes(metadata.runtimeMinutes());
        movie.setTmdbPosterPath(metadata.posterPath());
        movie.setTmdbBackdropPath(metadata.backdropPath());
        movie.setPosterPath("assets/movies/" + movie.getId() + "/poster.jpg" );
        movie.setBackdropPath("assets/movies/" + movie.getId() + "/backdrop.jpg" );
        genreService.syncGenres(movie, metadata.genres());

        movieRepository.save(movie);
        return movie;
    }
    private Movie findOrCreateByTmdbId(long tmdbId) {
        return movieRepository.findByTmdbId(tmdbId).orElseGet(() ->
        {
            Movie movie = new Movie();
            movie.setTmdbId(tmdbId);
            return movieRepository.save(movie);
        });
    }


}