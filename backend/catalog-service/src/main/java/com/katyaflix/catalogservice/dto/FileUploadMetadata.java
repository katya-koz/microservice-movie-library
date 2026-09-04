package com.katyaflix.catalogservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;


import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FileUploadMetadata {
    public enum FileType { VIDEO, SUBTITLE }
    public enum UploadType { MOVIE, SHOW }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UploadFileEntryDto(
            String key,
            FileType fileType,
            String filename
    ) {}

    public sealed interface UploadMetadataDto
            permits MovieUploadMetadataDto, ShowUploadMetadataDto {
        UploadType type();
        long tmdbId();
        List<UploadFileEntryDto> getFiles();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MovieUploadMetadataDto(
            long tmdbId,
            UUID id,
            UploadType type,
            String title,
            LocalDate releaseDate,
            String overview,
            String creatorNames,
            Integer runtimeMinutes,
            String posterPath,
            String backdropPath,
            List<UploadFileEntryDto> files
    ) implements UploadMetadataDto {
        @Override
        public List<UploadFileEntryDto> getFiles() { return files; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ShowUploadMetadataDto(
            long tmdbId,
            UUID id,
            UploadType type,
            String title,
            LocalDate firstAirDate,
            String overview,
            String creatorNames,
            String status,
            String posterPath,
            String backdropPath,
            List<SeasonUploadMetadataDto> seasons
    ) implements UploadMetadataDto {
        @Override
        public List<UploadFileEntryDto> getFiles() {
            return seasons.stream()
                    .flatMap(season -> season.episodes().stream())
                    .flatMap(episode -> episode.files().stream())
                    .toList();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SeasonUploadMetadataDto(
            Long tmdbId, // nullable
            UUID id,
            Integer seasonNumber,
            String title,
            String overview,
            LocalDate airDate,
            String posterPath,
            List<EpisodeUploadMetadataDto> episodes
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EpisodeUploadMetadataDto(
            Long tmdbId, // nullable
            UUID id,
            String title,
            LocalDate airDate,
            String overview,
            String creatorNames,
            Integer runtimeMinutes,
            Integer episodeNumber,
            String stillPath,
            List<UploadFileEntryDto> files
    ) {}
}
