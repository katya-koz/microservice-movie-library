package com.katyaflix.catalogservice.service;

import com.katyaflix.catalogservice.entity.Genre;
import com.katyaflix.catalogservice.entity.GenreToMedia;
import com.katyaflix.catalogservice.entity.Movie;
import com.katyaflix.catalogservice.entity.Show;
import com.katyaflix.catalogservice.repository.GenreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GenreService{

    private final GenreRepository genreRepository;

    @Transactional
    public Genre findOrCreate(String name) {
        String normalizedName = name.trim();

        return genreRepository.findByNameIgnoreCase(normalizedName)
                .orElseGet(() -> { Genre genre = new Genre();
                    genre.setName(normalizedName);
                    return genreRepository.save(genre);
                });
    }


    public void syncGenres(Show show, List<String> genreNames) {
        Set<String> desired = genreNames == null ? Set.of() : new HashSet<>(genreNames);

        // drop associations that are no longer wanted
        show.getGenres().removeIf(gtm -> !desired.contains(gtm.getGenre().getName()));

        Set<String> alreadyLinked = show.getGenres().stream()
                .map(gtm -> gtm.getGenre().getName())
                .collect(Collectors.toSet());

        for (String genreName : desired) {
            if (alreadyLinked.contains(genreName)) continue; // untouched, no delete+insert race

            Genre genre = findOrCreate(genreName);
            GenreToMedia relationship = new GenreToMedia();
            relationship.setGenre(genre);
            relationship.setShow(show);
            show.getGenres().add(relationship);
        }
    }

    public void syncGenres(Movie movie, List<String> genreNames) {
        Set<String> desired = genreNames == null ? Set.of() : new HashSet<>(genreNames);

        // drop associations that are no longer wanted
        movie.getGenres().removeIf(gtm -> !desired.contains(gtm.getGenre().getName()));

        Set<String> alreadyLinked = movie.getGenres().stream()
                .map(gtm -> gtm.getGenre().getName())
                .collect(Collectors.toSet());

        for (String genreName : desired) {
            if (alreadyLinked.contains(genreName)) continue; // untouched, no delete+insert race

            Genre genre = findOrCreate(genreName);
            GenreToMedia relationship = new GenreToMedia();
            relationship.setGenre(genre);
            relationship.setMovie(movie);
            movie.getGenres().add(relationship);
        }
    }
}