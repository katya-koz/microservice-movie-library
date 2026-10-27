package com.katyaflix.catalogservice.service;

import com.katyaflix.catalogservice.dto.enums.MediaCategory;
import com.katyaflix.catalogservice.dto.mediaenrichment.SubtitleEnrichment;
import com.katyaflix.catalogservice.entity.*;
import com.katyaflix.catalogservice.repository.EpisodeRepository;
import com.katyaflix.catalogservice.repository.MovieRepository;
import com.katyaflix.catalogservice.repository.SubtitleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubtitleService {
    private final SubtitleRepository subtitleRepository;
    private final MovieRepository movieRepository;
    private final EpisodeRepository episodeRepository;

    @Transactional
    public Subtitle upsertSubtitleFile(SubtitleEnrichment e, MediaCategory type) {
        boolean isMovie = type == MediaCategory.MOVIE;

        // identity = (parent media, original file name): re-uploading the same file updates it,
        // a same-named file on a different episode/movie is a different subtitle
        Optional<Subtitle> existing = isMovie
                ? subtitleRepository.findByMovieIdAndOriginalFileName(e.id(), e.originalFilename())
                : subtitleRepository.findByEpisodeIdAndOriginalFileName(e.id(), e.originalFilename());

        Subtitle subtitle = existing.orElseGet(() -> {
            Subtitle s = new Subtitle();
            s.setOriginalFileName(e.originalFilename());
            s.setCreatedAt(Instant.now());          // only for new rows
            if (isMovie) {
                s.setMovie(movieRepository.findById(e.id())
                        .orElseThrow(() -> new IllegalArgumentException("Unknown movie " + e.id())));
            } else {
                s.setEpisode(episodeRepository.findById(e.id())
                        .orElseThrow(() -> new IllegalArgumentException("Unknown episode " + e.id())));
            }
            return s;
        });

        subtitle.setFilePath(e.filePath());
        subtitle.setLanguageCode(e.languageCode());
        subtitle.setLabel(e.label());
        subtitle.setFormat(e.format());
        return subtitleRepository.save(subtitle);
    }
}
