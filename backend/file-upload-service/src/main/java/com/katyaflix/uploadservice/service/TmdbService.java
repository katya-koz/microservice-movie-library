package com.katyaflix.uploadservice.service;

import com.katyaflix.uploadservice.dto.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * server side proxy for tmdb's data api
 */
@Service
public class TmdbService {

    private static final String TMDB_API_BASE = "https://api.themoviedb.org/3";

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public TmdbService(ObjectMapper objectMapper, @Value("${tmdb.api-key:}") String apiKey) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
    }

    public FileUploadMetadata.UploadMetadataDto populateMedia(FileUploadMetadata.UploadMetadataDto metadata)
    {
        requireApiKey();

        if (metadata.type() == FileUploadMetadata.UploadType.SHOW) {
            return showPopulate((FileUploadMetadata.ShowUploadMetadataDto) metadata);
        }

        return moviePopulate((FileUploadMetadata.MovieUploadMetadataDto) metadata);
    }

    private FileUploadMetadata.MovieUploadMetadataDto moviePopulate(FileUploadMetadata.MovieUploadMetadataDto upload)
    {
        String url = TMDB_API_BASE + "/movie/" + upload.tmdbId() + "?api_key=" + encode(apiKey) + "&append_to_response=credits";
        JsonNode movieData = getJson(url);

        String title = textOrNull(movieData, "title");
        List<String> genres = new ArrayList<>();
        for (JsonNode g : movieData.path("genres")) {
            genres.add(g.path("name").asText());
        }
        LocalDate releaseDate = parseDate(textOrNull(movieData, "release_date"));
        String overview = textOrDefault(movieData, "overview", "");
        String posterPath = textOrNull(movieData, "poster_path");
        String backdropPath = textOrNull(movieData, "backdrop_path");
        List<String> creators = new ArrayList<>();
        for (JsonNode crewMember : movieData.path("credits").path("crew")) {

            if ("Director".equals(textOrNull(crewMember, "job"))) {
                String name = textOrNull(crewMember, "name");

                if (name != null && !name.isBlank()) {
                    creators.add(name);
                }
            }
        }

        Integer runtimeMinutes = null;

        JsonNode runtimeNode = movieData.path("runtime");

        if (runtimeNode.isNumber()) {
            runtimeMinutes = runtimeNode.asInt();
        }

        return new FileUploadMetadata.MovieUploadMetadataDto(
                upload.tmdbId(),
                null,
                FileUploadMetadata.UploadType.MOVIE,
                title,
                releaseDate,
                overview,
                String.join(", ", creators),
                runtimeMinutes,
                posterPath,
                backdropPath,
                upload.files(),
                genres
        );
    }

    private FileUploadMetadata.ShowUploadMetadataDto showPopulate(FileUploadMetadata.ShowUploadMetadataDto upload)
    {
        String url = TMDB_API_BASE + "/tv/" + upload.tmdbId() + "?api_key=" + encode(apiKey);

        JsonNode showData = getJson(url);
        List<String> genres = new ArrayList<>();
        for (JsonNode g : showData.path("genres")) {
            genres.add(g.path("name").asText());
        }
        String title = textOrNull(showData, "name");
        LocalDate firstAirDate = parseDate(textOrNull(showData, "first_air_date"));
        String overview = textOrDefault(showData, "overview", "");
        String status = textOrNull(showData, "status");
        String posterPath = textOrNull(showData, "poster_path");
        String backdropPath = textOrNull(showData, "backdrop_path");

        // tv creators
        List<String> creators = new ArrayList<>();

        for (JsonNode creator : showData.path("created_by")) {
            String name = textOrNull(creator, "name");

            if (name != null && !name.isBlank()) {
                creators.add(name);
            }
        }

        // 1 tmdb request per season (refactored from requesting every episode!)
        List<FileUploadMetadata.SeasonUploadMetadataDto> seasons = new ArrayList<>();

        for (FileUploadMetadata.SeasonUploadMetadataDto uploadSeason : upload.seasons()) {

            String seasonUrl = TMDB_API_BASE + "/tv/" + upload.tmdbId() + "/season/" + uploadSeason.seasonNumber() + "?api_key=" + encode(apiKey);
            JsonNode seasonData = getJson(seasonUrl);
            long tmdbSeasonId = seasonData.path("id").asLong();
            String seasonTitle = textOrNull(seasonData, "name");
            String seasonOverview = textOrDefault(seasonData, "overview", "");
            LocalDate seasonAirDate = parseDate(textOrNull(seasonData, "air_date"));
            String seasonPosterPath = textOrNull(seasonData, "poster_path");

            List<FileUploadMetadata.EpisodeUploadMetadataDto> episodes = new ArrayList<>();

            for (JsonNode tmdbEpisode : seasonData.path("episodes")) {
                int episodeNumber = tmdbEpisode.path("episode_number").asInt();

                FileUploadMetadata.EpisodeUploadMetadataDto uploadEpisode =
                        uploadSeason.episodes().stream()
                                .filter(e ->
                                        e.episodeNumber() != null && e.episodeNumber() == episodeNumber
                                ).findFirst().orElse(null);

                if (uploadEpisode == null) {
                    continue;
                }

                String episodeTitle = textOrNull(tmdbEpisode, "name");

                LocalDate episodeAirDate = parseDate(textOrNull(tmdbEpisode, "air_date"));

                String episodeOverview = textOrDefault(tmdbEpisode, "overview", "");

                String stillPath = textOrNull(tmdbEpisode, "still_path");

                List<String> episodeCreators = new ArrayList<>();

                for (JsonNode crewMember : tmdbEpisode.path("crew")) {
                    String job = textOrNull(crewMember, "job");

                    if ("Director".equals(job) || "Writer".equals(job)) {

                        String name = textOrNull(crewMember, "name");

                        if (name != null && !name.isBlank()) {
                            episodeCreators.add(name);
                        }
                    }
                }

                Integer runtimeMinutes = null;

                JsonNode runtimeNode = tmdbEpisode.path("runtime");

                if (runtimeNode.isNumber()) {
                    runtimeMinutes = runtimeNode.asInt();
                }

                episodes.add(
                        new FileUploadMetadata.EpisodeUploadMetadataDto(
                                tmdbEpisode.path("id").asLong(),
                                null,
                                episodeTitle,
                                episodeAirDate,
                                episodeOverview,
                                String.join(", ", episodeCreators),
                                runtimeMinutes,
                                episodeNumber,
                                stillPath,
                                uploadEpisode.files()
                        )
                );
            }
            seasons.add(
                    new FileUploadMetadata.SeasonUploadMetadataDto(
                            tmdbSeasonId,
                            null,
                            uploadSeason.seasonNumber(),
                            seasonTitle,
                            seasonOverview,
                            seasonAirDate,
                            seasonPosterPath,
                            episodes
                    )
            );
        }

        return new FileUploadMetadata.ShowUploadMetadataDto(
                upload.tmdbId(),
                null,
                FileUploadMetadata.UploadType.SHOW,
                title,
                firstAirDate,
                overview,
                String.join(", ", creators),
                status,
                posterPath,
                backdropPath,
                seasons,
                genres
        );
    }

    public TmdbSearchResponseDto search(String type, String query) {
        requireApiKey();
        String resolvedType = "tv".equals(type) ? "tv" : "movie";

        if (query == null || query.isBlank()) {
            return new TmdbSearchResponseDto(List.of());
        }

        String url = TMDB_API_BASE + "/search/" + resolvedType + "?api_key=" + encode(apiKey) + "&query=" + encode(query) + "&include_adult=false";

        JsonNode data = getJson(url);
        boolean isTv = "tv".equals(resolvedType);

        List<TmdbSearchResultDto> results = new ArrayList<>();
        for (JsonNode r : data.path("results")) {
            if (results.size() >= 8) break;
            String rawDate = textOrNull(r, isTv ? "first_air_date" : "release_date");
            String year = (rawDate != null && rawDate.length() >= 4) ? rawDate.substring(0, 4) : "—";
            results.add(
                    new TmdbSearchResultDto(
                        r.path("id").asLong(),
                        isTv ? textOrNull(r, "name") : textOrNull(r, "title"),
                        year,
                        rawDate,
                        textOrNull(r, "poster_path"),
                        textOrDefault(r, "overview", "")
                    )
            );
        }
        return new TmdbSearchResponseDto(results);
    }

    public TmdbDetailsDto details(String type, long id) {
        requireApiKey();
        boolean isTv = "tv".equals(type);

        String url = isTv
                ? TMDB_API_BASE + "/tv/" + id + "?api_key=" + encode(apiKey)
                : TMDB_API_BASE + "/movie/" + id + "?api_key=" + encode(apiKey) + "&append_to_response=credits";

        JsonNode data = getJson(url);

        List<String> genres = new ArrayList<>();
        for (JsonNode g : data.path("genres")) {
            genres.add(g.path("name").asText());
        }

        List<String> creators = new ArrayList<>();
        if (isTv) {
            for (JsonNode c : data.path("created_by")) {
                creators.add(c.path("name").asText());
            }
        } else {
            for (JsonNode c : data.path("credits").path("crew")) {
                if ("Director".equals(textOrNull(c, "job"))) {
                    creators.add(c.path("name").asText());
                }
            }
        }

        Integer runtime;
        JsonNode runtimeNode = data.path("runtime");
        if (!runtimeNode.isMissingNode() && !runtimeNode.isNull()) {
            runtime = runtimeNode.asInt();
        } else {
            JsonNode episodeRuntimes = data.path("episode_run_time");
            runtime = (episodeRuntimes.isArray() && episodeRuntimes.size() > 0) ? episodeRuntimes.get(0).asInt() : null;
        }

        return new TmdbDetailsDto(
                genres,
                runtime,
                textOrNull(data, "backdrop_path"),
                textOrDefault(data, "overview", ""),
                creators,
                isTv ? textOrNull(data, "status") : null
        );
    }

    public TmdbSeasonResponseDto season(long tvId, int seasonNumber) {
        requireApiKey();
        String url = TMDB_API_BASE + "/tv/" + tvId + "/season/" + seasonNumber + "?api_key=" + encode(apiKey);

        JsonNode data;
        try {
            data = getJson(url);
        } catch (TmdbRequestException e) {
            return new TmdbSeasonResponseDto(List.of());
        }

        List<TmdbSeasonEpisodeDto> episodes = new ArrayList<>();
        for (JsonNode e : data.path("episodes")) {
            episodes.add(new TmdbSeasonEpisodeDto(
                    e.path("episode_number").asInt(),
                    textOrNull(e, "name"),
                    textOrDefault(e, "overview", ""),
                    textOrNull(e, "still_path")
            ));
        }
        return new TmdbSeasonResponseDto(episodes);
    }

    private void requireApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new TmdbRequestException("TMDB_API_KEY is not configured on the server.", 500);
        }
    }

    private JsonNode getJson(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                throw new TmdbRequestException(
                        "TMDB request failed with HTTP " + response.statusCode() + ": " + response.body(), response.statusCode()
                );
            }

            return objectMapper.readTree(response.body());

        } catch (TmdbRequestException e) {
            throw e;

        } catch (IOException e) {
            throw new TmdbRequestException("Failed to reach TMDB", 502, e);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new TmdbRequestException("Interrupted while calling TMDB", 500, e);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return (value.isMissingNode() || value.isNull()) ? null : value.asText();
    }

    private static String textOrDefault(JsonNode node, String field, String defaultValue) {
        String value = textOrNull(node, field);
        return value != null ? value : defaultValue;
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
