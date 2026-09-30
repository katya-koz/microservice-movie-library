package com.katyaflix.uploadservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
        List<String> genres();
    }

    /** Mirrors UploadMetadata in the frontend's lib/uploadPayload.ts.
     *  Reused end-to-end: incoming request -> TMDB-populated -> stored job payload -> finalization input. */
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
            List<UploadFileEntryDto> files,
            List<String> genres
    ) implements UploadMetadataDto {
        @Override
        public List<UploadFileEntryDto> getFiles() { return files; }

        @Override
        public List<String> genres() {
            return genres;
        }
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
            List<SeasonUploadMetadataDto> seasons,
            List<String> genres
    ) implements UploadMetadataDto {
        @Override
        public List<UploadFileEntryDto> getFiles() {
            return seasons.stream()
                    .flatMap(season -> season.episodes().stream())
                    .flatMap(episode -> episode.files().stream())
                    .toList();
        }
        @Override
        public List<String> genres() {
            return genres;
        }


        public EpisodeUploadMetadataDto getEpisodeByTmdbId(Long tmdbId) {
            return seasons.stream()
                    .flatMap(season -> season.episodes().stream())
                    .filter(episode -> tmdbId.equals(episode.tmdbId()))
                    .findFirst()
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Episode not found with TMDB ID: " + tmdbId
                            )
                    );
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
