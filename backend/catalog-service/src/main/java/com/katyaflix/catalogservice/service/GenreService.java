package com.katyaflix.catalogservice.service;

import com.katyaflix.catalogservice.entity.Genre;
import com.katyaflix.catalogservice.repository.GenreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GenreService {

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
}