package com.katyaflix.catalogservice.service;

import com.katyaflix.catalogservice.dto.metadata.EpisodeMetadata;
import com.katyaflix.catalogservice.entity.Episode;
import com.katyaflix.catalogservice.entity.Season;
import com.katyaflix.catalogservice.repository.EpisodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EpisodeService {
    private final EpisodeRepository episodeRepository;

    public Episode upsertEpisode(EpisodeMetadata metadata, Season season) {
        Episode episode = findOrCreateByTmdbId(metadata.tmdbId());

        episode.setTmdbId(metadata.tmdbId());
        episode.setSeason(season);
        episode.setTitle(metadata.title());
        episode.setAirDate(metadata.airDate());
        episode.setOverview(metadata.overview());
        episode.setRuntimeMinutes(metadata.runtimeMinutes());
        episode.setEpisodeNumber(metadata.episodeNumber());
        episode.setTmdbStillPath(metadata.stillPath());
        episode.setStillPath("assets/shows/" + season.getShow().getId() + "/seasons/" + season.getId() + "/episodes/" + episode.getId() + "/still.jpg" );

        episodeRepository.save(episode);
        return episode;
    }

    private Episode findOrCreateByTmdbId(long tmdbId) {
        return episodeRepository.findByTmdbId(tmdbId).orElseGet(() ->
        {
            Episode season = new Episode();
            season.setTmdbId(tmdbId);
            return episodeRepository.save(season);
        });
    }
}
