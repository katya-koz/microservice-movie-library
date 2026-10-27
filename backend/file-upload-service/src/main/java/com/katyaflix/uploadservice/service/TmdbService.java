package com.katyaflix.uploadservice.service;

import com.katyaflix.uploadservice.dto.*;
import com.katyaflix.uploadservice.dto.enums.MediaCategory;
import com.katyaflix.uploadservice.dto.metadata.*;
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

    public MediaMetadata populateMedia(
            MediaCategory mediaType,
            long tmdbId
    ) {
        requireApiKey();

        if (mediaType == MediaCategory.SHOW) {
            return showPopulate(tmdbId);
        }

        return moviePopulate(tmdbId);
    }


    private MovieMetadata moviePopulate(long tmdbId) {

        String url = TMDB_API_BASE
                + "/movie/"
                + tmdbId
                + "?api_key="
                + encode(apiKey)
                + "&append_to_response=credits";

        JsonNode movieData = getJson(url);

        String title = textOrNull(movieData, "title");

        List<String> genres = new ArrayList<>();

        for (JsonNode genre : movieData.path("genres")) {
            String name = textOrNull(genre, "name");

            if (name != null && !name.isBlank()) {
                genres.add(name);
            }
        }

        LocalDate releaseDate =
                parseDate(textOrNull(movieData, "release_date"));

        String overview =
                textOrDefault(movieData, "overview", "");

        String posterPath =
                textOrNull(movieData, "poster_path");

        String backdropPath =
                textOrNull(movieData, "backdrop_path");


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


        return new MovieMetadata(
                tmdbId,
                null,                                      // catalogId
                MediaCategory.MOVIE,
                title,
                releaseDate,
                overview,
                String.join(", ", creators),
                runtimeMinutes,
                posterPath,
                backdropPath,
                genres
        );
    }


    private ShowMetadata showPopulate(long tmdbId) {

        String url = TMDB_API_BASE
                + "/tv/"
                + tmdbId
                + "?api_key="
                + encode(apiKey);

        JsonNode showData = getJson(url);


        /*
         * Show-level metadata
         */

        String title =
                textOrNull(showData, "name");

        LocalDate firstAirDate =
                parseDate(textOrNull(showData, "first_air_date"));

        String overview =
                textOrDefault(showData, "overview", "");

        String status =
                textOrNull(showData, "status");

        String posterPath =
                textOrNull(showData, "poster_path");

        String backdropPath =
                textOrNull(showData, "backdrop_path");


        /*
         * Genres
         */

        List<String> genres = new ArrayList<>();

        for (JsonNode genre : showData.path("genres")) {

            String name = textOrNull(genre, "name");

            if (name != null && !name.isBlank()) {
                genres.add(name);
            }
        }


        /*
         * Show creators
         */

        List<String> creators = new ArrayList<>();

        for (JsonNode creator : showData.path("created_by")) {

            String name = textOrNull(creator, "name");

            if (name != null && !name.isBlank()) {
                creators.add(name);
            }
        }


        /*
         * Populate EVERY season and EVERY episode.
         *
         * We intentionally do not use the uploaded seasons/episodes
         * here. TMDB is the source of truth for catalog metadata.
         */

        List<SeasonMetadata> seasons = new ArrayList<>();

        for (JsonNode seasonSummary : showData.path("seasons")) {

            int seasonNumber =
                    seasonSummary.path("season_number").asInt();


            /*
             * TMDB's main TV response gives us the list of seasons,
             * but the season endpoint gives us the complete episode data.
             */

            String seasonUrl = TMDB_API_BASE
                    + "/tv/"
                    + tmdbId
                    + "/season/"
                    + seasonNumber
                    + "?api_key="
                    + encode(apiKey);

            JsonNode seasonData = getJson(seasonUrl);


            Long tmdbSeasonId = null;

            JsonNode seasonIdNode = seasonData.path("id");

            if (seasonIdNode.isNumber()) {
                tmdbSeasonId = seasonIdNode.asLong();
            }


            String seasonTitle =
                    textOrNull(seasonData, "name");

            String seasonOverview =
                    textOrDefault(seasonData, "overview", "");

            LocalDate seasonAirDate =
                    parseDate(textOrNull(seasonData, "air_date"));

            String seasonPosterPath =
                    textOrNull(seasonData, "poster_path");


            /*
             * Every episode returned by TMDB is included.
             */

            List<EpisodeMetadata> episodes = new ArrayList<>();

            for (JsonNode tmdbEpisode : seasonData.path("episodes")) {

                Long tmdbEpisodeId = null;

                JsonNode episodeIdNode = tmdbEpisode.path("id");

                if (episodeIdNode.isNumber()) {
                    tmdbEpisodeId = episodeIdNode.asLong();
                }


                Integer episodeNumber = null;

                JsonNode episodeNumberNode =
                        tmdbEpisode.path("episode_number");

                if (episodeNumberNode.isNumber()) {
                    episodeNumber = episodeNumberNode.asInt();
                }


                String episodeTitle =
                        textOrNull(tmdbEpisode, "name");

                LocalDate episodeAirDate =
                        parseDate(textOrNull(tmdbEpisode, "air_date"));

                String episodeOverview =
                        textOrDefault(tmdbEpisode, "overview", "");

                String stillPath =
                        textOrNull(tmdbEpisode, "still_path");


                /*
                 * Episode directors / writers
                 */

                List<String> episodeCreators = new ArrayList<>();

                for (JsonNode crewMember : tmdbEpisode.path("crew")) {

                    String job =
                            textOrNull(crewMember, "job");

                    if ("Director".equals(job)
                            || "Writer".equals(job)) {

                        String name =
                                textOrNull(crewMember, "name");

                        if (name != null && !name.isBlank()) {
                            episodeCreators.add(name);
                        }
                    }
                }


                /*
                 * Episode runtime
                 */

                Integer runtimeMinutes = null;

                JsonNode runtimeNode =
                        tmdbEpisode.path("runtime");

                if (runtimeNode.isNumber()) {
                    runtimeMinutes = runtimeNode.asInt();
                }


                episodes.add(
                        new EpisodeMetadata(
                                tmdbEpisodeId,
                                null,                       // catalogId
                                episodeTitle,
                                episodeAirDate,
                                episodeOverview,
                                String.join(", ", episodeCreators),
                                runtimeMinutes,
                                episodeNumber,
                                stillPath
                        )
                );
            }


            seasons.add(
                    new SeasonMetadata(
                            tmdbSeasonId,
                            null,                          // catalogId
                            seasonNumber,
                            seasonTitle,
                            seasonOverview,
                            seasonAirDate,
                            seasonPosterPath,
                            episodes
                    )
            );
        }


        return new ShowMetadata(
                tmdbId,
                null,                                      // catalogId
                MediaCategory.SHOW,
                title,
                firstAirDate,
                overview,
                String.join(", ", creators),
                status,
                posterPath,
                backdropPath,
                genres,
                seasons
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
