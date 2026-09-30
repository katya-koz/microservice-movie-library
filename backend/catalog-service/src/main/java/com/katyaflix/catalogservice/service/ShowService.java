package com.katyaflix.catalogservice.service;

import com.katyaflix.catalogservice.dto.CatalogDtos;
import com.katyaflix.catalogservice.dto.CatalogDtos.EpisodeDetail;
import com.katyaflix.catalogservice.dto.CatalogDtos.SeasonSummary;
import com.katyaflix.catalogservice.dto.CatalogDtos.ShowDetail;
import com.katyaflix.catalogservice.dto.CatalogDtos.ShowSummary;
import com.katyaflix.catalogservice.entity.*;
import com.katyaflix.catalogservice.repository.*;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShowService {

    @Value("${media.url}")
    private String mediaUrlRoot;
    private final ShowRepository showRepository;
    private final SeasonRepository seasonRepository;
    private final EpisodeRepository episodeRepository;
    private final MediaFileRepository mediaFileRepository;

    public Page<ShowSummary> getCatalogPage(String title, Pageable pageable) {
        Page<ShowRepository.ShowCatalogProjection> shows;

        if (title == null || title.isBlank()) {
            shows = showRepository.findAllProjectedBy(pageable);
        } else {
            shows = showRepository.findByTitleContainingIgnoreCase(title, pageable);
        }

        return shows.map(p ->  new CatalogDtos.ShowSummary( p.getId(), p.getTitle(), mediaUrlRoot + p.getPosterPath() ) );
    }

    public Optional<ShowDetail> getDetail(UUID showId) {
        return showRepository.findById(showId).map(this::toDetail);
    }

    public List<EpisodeDetail> getEpisodesForSeason(UUID showId, Integer seasonNumber) {
        Season season = seasonRepository.findByShowIdAndSeasonNumber(showId, seasonNumber).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Season " + seasonNumber + " not found for this show"));

        return episodeRepository.findBySeasonIdOrderByEpisodeNumberAsc(season.getId()).stream().map(e -> toEpisodeDetail(e, seasonNumber)).toList();
    }
    public List<CatalogDtos.ShowSummary> getSummaries(List<UUID> ids) {
        return showRepository.findByIdIn(ids).stream().map(p -> new CatalogDtos.ShowSummary(p.getId(), p.getTitle(),mediaUrlRoot + p.getPosterPath())).toList();
    }
    private ShowDetail toDetail(Show show) {
        List<SeasonSummary> seasons = seasonRepository
                .findByShowIdOrderBySeasonNumberAsc(show.getId()).stream()
                .map(s -> new SeasonSummary(
                        s.getId(),
                        s.getSeasonNumber(),
                        s.getTitle(),
                        mediaUrlRoot +  s.getPosterPath(),
                        episodeRepository.countBySeasonId(s.getId())
                ))
                .toList();

        return new ShowDetail(
                show.getId(),
                show.getTitle(),
                show.getOverview(),
                show.getFirstAirDate(),
                show.getCreatorNames(),
                show.getStatus(),
                mediaUrlRoot + show.getPosterPath(),
                mediaUrlRoot + show.getBackdropPath(),
                seasons,
                show.getGenres().stream().map(GenreToMedia::getGenre).map(Genre::getName).toList()
        );
    }

    private EpisodeDetail toEpisodeDetail(Episode e, Integer seasonNumber) {
        return new EpisodeDetail(
                e.getId(),
                e.getEpisodeNumber(),
                seasonNumber,
                e.getTitle(),
                e.getOverview(),
                e.getRuntimeMinutes(),
                mediaUrlRoot + e.getStillPath(),
                e.getAirDate()
        );
    }
}