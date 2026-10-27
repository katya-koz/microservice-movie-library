package com.katyaflix.catalogservice.service;

import com.katyaflix.catalogservice.dto.metadata.EpisodeMetadata;
import com.katyaflix.catalogservice.dto.metadata.SeasonMetadata;
import com.katyaflix.catalogservice.entity.Episode;
import com.katyaflix.catalogservice.entity.Season;
import com.katyaflix.catalogservice.entity.Show;
import com.katyaflix.catalogservice.repository.SeasonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeasonService {
    private final SeasonRepository seasonRepository;
    private final EpisodeService episodeService;

    public Season upsertSeason(SeasonMetadata metadata, Show show) {
        Season season = findOrCreateByTmdbId(metadata.tmdbId());
        season.setTmdbId(metadata.tmdbId());
        season.setShow(show);
        season.setSeasonNumber(metadata.seasonNumber());
        season.setTitle(metadata.title());
        season.setOverview(metadata.overview());
        season.setAirDate(metadata.airDate());
        season.setTmdbPosterPath(metadata.posterPath());
        season.setPosterPath("assets/shows/" + season.getShow().getId() + "/seasons/" + season.getId() + "/poster.jpg" );
        seasonRepository.save(season);

        List<Episode> episodes = new ArrayList<>();
        for (EpisodeMetadata episodeDto : metadata.episodes()) {
            episodes.add(episodeService.upsertEpisode(episodeDto, season));
        }
        return season;
    }


    private Season findOrCreateByTmdbId(long tmdbId) {
        return seasonRepository.findByTmdbId(tmdbId).orElseGet(() ->
        {
            Season season = new Season();
            season.setTmdbId(tmdbId);
            return seasonRepository.save(season);
        });
    }
}
