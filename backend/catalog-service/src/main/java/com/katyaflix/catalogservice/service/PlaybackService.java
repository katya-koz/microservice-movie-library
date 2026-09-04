package com.katyaflix.catalogservice.service;

import com.katyaflix.catalogservice.dto.CatalogDtos.NextEpisode;
import com.katyaflix.catalogservice.dto.CatalogDtos.PlaybackInfo;
import com.katyaflix.catalogservice.dto.CatalogDtos.SubtitleTrack;
import com.katyaflix.catalogservice.entity.Episode;
import com.katyaflix.catalogservice.entity.MediaFile;
import com.katyaflix.catalogservice.entity.Season;
import com.katyaflix.catalogservice.repository.EpisodeRepository;
import com.katyaflix.catalogservice.repository.MediaFileRepository;
import com.katyaflix.catalogservice.repository.SeasonRepository;
import com.katyaflix.catalogservice.repository.SubtitleRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlaybackService {

    private final MediaFileRepository mediaFileRepository;
    private final SubtitleRepository subtitleRepository;
    private final EpisodeRepository episodeRepository;
    private final SeasonRepository seasonRepository;

    public PlaybackInfo resolveForMovie(UUID movieId) {
        MediaFile file = mediaFileRepository.findByMovieId(movieId)
                // 404 rather than an empty/broken player
                // expected state while a file is still uploading/encoding. not an error
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"No playable file yet for this movie — it may still be encoding"));
        return toPlaybackInfo(file);
    }

    public PlaybackInfo resolveForEpisode(UUID episodeId) {
        MediaFile file = mediaFileRepository.findByEpisodeId(episodeId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "No playable file yet for this episode — it may still be encoding"));
        return toPlaybackInfo(file);
    }

    /**
     * same season, next episode number.
     *
     * if this was the last episode of the season roll over to episode 1 of the next season
     *
     * if this was the last episode of the show entirely - theres nothing to autoplay next
     */
    public NextEpisode getNext(UUID currentEpisodeId) {
        Episode current = episodeRepository.findByIdWithSeasonAndShow(currentEpisodeId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Episode not found"));
        Season currentSeason = current.getSeason();
        UUID showId = currentSeason.getShow().getId();
        Optional<Episode> next = episodeRepository.findFirstBySeasonIdAndEpisodeNumberGreaterThanOrderByEpisodeNumberAsc(currentSeason.getId(), current.getEpisodeNumber());
        if (next.isPresent()) {
            return toNextEpisode(next.get(), currentSeason.getSeasonNumber());
        }
        // Roll over into the next season
        Optional<Season> nextSeason = seasonRepository.findFirstByShowIdAndSeasonNumberGreaterThanOrderBySeasonNumberAsc(showId, currentSeason.getSeasonNumber());
        if (nextSeason.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No next episode — this is the last episode of the show");
        }
        Episode firstOfNextSeason = episodeRepository.findFirstBySeasonIdOrderByEpisodeNumberAsc(nextSeason.get().getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Next season has no episodes cataloged yet"));
        return toNextEpisode(firstOfNextSeason, nextSeason.get().getSeasonNumber());
    }

    private PlaybackInfo toPlaybackInfo(MediaFile file) {
        List<SubtitleTrack> tracks = subtitleRepository.findByMediaFileId(file.getId()).stream()
                .map(s -> new SubtitleTrack(s.getId(), s.getLanguageCode(), s.getLabel(), s.getFilePath(),s.isForced(), s.isSdh(), s.isDefault()))
                .toList();

        return new PlaybackInfo(
                file.getId(),
                file.getFilePath(),
                file.getContainerFormat(),
                file.getDurationSeconds(),
                file.getResolution(),
                tracks
        );
    }

    private NextEpisode toNextEpisode(Episode e, Integer seasonNumber) {
        return new NextEpisode(
                e.getId(),
                e.getSeason().getShow().getId(),
                seasonNumber,
                e.getEpisodeNumber(),
                e.getTitle(),
                e.getStillPath()
        );
    }
}