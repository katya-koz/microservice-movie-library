package com.katyaflix.catalogservice.service;

import com.katyaflix.catalogservice.dto.CatalogDtos.EpisodeDetail;
import com.katyaflix.catalogservice.dto.CatalogDtos.SeasonSummary;
import com.katyaflix.catalogservice.dto.CatalogDtos.ShowDetail;
import com.katyaflix.catalogservice.dto.CatalogDtos.ShowSummary;
import com.katyaflix.catalogservice.entity.Episode;
import com.katyaflix.catalogservice.entity.Season;
import com.katyaflix.catalogservice.entity.Show;
import com.katyaflix.catalogservice.repository.EpisodeRepository;
import com.katyaflix.catalogservice.repository.MediaFileRepository;
import com.katyaflix.catalogservice.repository.SeasonRepository;
import com.katyaflix.catalogservice.repository.ShowRepository;

import lombok.RequiredArgsConstructor;
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

    private final ShowRepository showRepository;
    private final SeasonRepository seasonRepository;
    private final EpisodeRepository episodeRepository;
    private final MediaFileRepository mediaFileRepository;

    public Page<ShowSummary> getCatalogPage(Pageable pageable) {
        return showRepository.findAllProjectedBy(pageable).map(p -> new ShowSummary(p.getId(), p.getTitle(), p.getPosterPath()));
    }

    public Optional<ShowDetail> getDetail(UUID showId) {
        return showRepository.findById(showId).map(this::toDetail);
    }

    public List<EpisodeDetail> getEpisodesForSeason(UUID showId, Integer seasonNumber) {
        Season season = seasonRepository.findByShowIdAndSeasonNumber(showId, seasonNumber).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Season " + seasonNumber + " not found for this show"));

        return episodeRepository.findBySeasonIdOrderByEpisodeNumberAsc(season.getId()).stream().map(e -> toEpisodeDetail(e, seasonNumber)).toList();
    }

    private ShowDetail toDetail(Show show) {
        List<SeasonSummary> seasons = seasonRepository
                .findByShowIdOrderBySeasonNumberAsc(show.getId()).stream()
                .map(s -> new SeasonSummary(
                        s.getId(),
                        s.getSeasonNumber(),
                        s.getTitle(),
                        s.getPosterPath(),
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
                show.getPosterPath(),
                show.getBackdropPath(),
                seasons
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
                e.getStillPath(),
                e.getAirDate()
        );
    }
}