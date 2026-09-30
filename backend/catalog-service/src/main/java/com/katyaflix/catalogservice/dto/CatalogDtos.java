package com.katyaflix.catalogservice.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * All response DTOs grouped in one file
 */
public class CatalogDtos {

    // lightweight views for catalog grids

    public record MovieSummary(UUID id, String title, String posterPath) {}

    public record ShowSummary(UUID id, String title, String posterPath) {}

    // full detail views

    public record MovieDetail(
            UUID id,
            String title,
            String overview,
            Integer runtimeMinutes,
            LocalDate releaseDate,
            String creatorNames,     // comma separated
            String posterPath,
            String backdropPath,
            List<String> genres
    ) {}

    public record ShowDetail(
            UUID id,
            String title,
            String overview,
            LocalDate firstAirDate,
            String creatorNames,
            String status,
            String posterPath,
            String backdropPath,
            List<SeasonSummary> seasons,   // season picker —> not full episode lists
            List<String> genres
    ) {}

    public record SeasonSummary(
            UUID seasonId,
            Integer seasonNumber,
            String title,
            String posterPath,
            long episodeCount
    ) {}

    public record EpisodeDetail(
            UUID id,
            Integer episodeNumber,
            Integer seasonNumber,
            String title,
            String overview,
            Integer runtimeMinutes,
            String stillPath,
            LocalDate airDate
    ) {}

    // playback

    public record SubtitleTrack(
            UUID id,
            String languageCode,
            String label,
            String filePath,
            boolean isForced,
            boolean isSdh,
            boolean isDefault
    ) {}

    public record PlaybackInfo(
            UUID mediaFileId,
            String filePath,
            String containerFormat,
            Integer durationSeconds,
            String resolution,
            List<SubtitleTrack> subtitles,
            UUID seasonId,
            UUID showId

    ) {}

    public record NextEpisode(
            UUID episodeId,
            UUID showId,
            Integer seasonNumber,
            Integer episodeNumber,
            String title,
            String stillPath
    ) {}
}