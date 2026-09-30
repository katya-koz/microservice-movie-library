package com.katyaflix.catalogservice.service;

import com.katyaflix.catalogservice.dto.CatalogEventDtos;
import com.katyaflix.catalogservice.dto.FileUploadMetadata;
import com.katyaflix.catalogservice.entity.*;
import com.katyaflix.catalogservice.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * tmdb id is used as the source of truth before items get their uuids assigned
 */
@Service
public class CatalogUpsertService {

    private final MovieRepository movieRepository;
    private final ShowRepository showRepository;
    private final SeasonRepository seasonRepository;
    private final EpisodeRepository episodeRepository;
    private final MediaFileRepository mediaFileRepository;
    private final SubtitleRepository subtitleRepository;
    private final GenreService genreService;

    public CatalogUpsertService(
            MovieRepository movieRepository,
            ShowRepository showRepository,
            SeasonRepository seasonRepository,
            EpisodeRepository episodeRepository,
            MediaFileRepository mediaFileRepository,
            SubtitleRepository subtitleRepository,
            GenreService genreService) {
        this.movieRepository = movieRepository;
        this.showRepository = showRepository;
        this.seasonRepository = seasonRepository;
        this.episodeRepository = episodeRepository;
        this.mediaFileRepository = mediaFileRepository;
        this.subtitleRepository = subtitleRepository;
        this.genreService = genreService;
    }

    @Transactional
    public Movie upsertMovie(FileUploadMetadata.MovieUploadMetadataDto metadata) {
        validateMovie(metadata);

//        System.out.println(metadata.toString());

//        Movie movie = findOrCreate(movieRepository::findByTmdbId, Movie::new, event.tmdbId());
        Movie movie = findOrCreate( // hydrate uuid because it is needed for the paths
                movieRepository::findByTmdbId,
                Movie::new,
                metadata.tmdbId()
        );
        movie.setTmdbId(metadata.tmdbId());
        movie.setTitle(metadata.title());
        movie.setReleaseDate(metadata.releaseDate());
        movie.setOverview(metadata.overview());
        movie.setCreatorNames(metadata.creatorNames());
        movie.setRuntimeMinutes(metadata.runtimeMinutes());
        movie.setTmdbPosterPath(metadata.posterPath());
        movie.setTmdbBackdropPath(metadata.backdropPath());

//        movie.setPosterPath("/assets/movies/" + movie.getId() + "/poster.jpg");
//        movie.setBackdropPath("/assets/movies/" + movie.getId() + "/backdrop.jpg");
        syncGenres(movie, metadata.genres());
        saveWithRaceRetry(movieRepository, movie, movieRepository::findByTmdbId, metadata.tmdbId());

        return movie;
    }

    @Transactional
    public MediaFile upsertMovieMediaFile(CatalogEventDtos.MediaFileEnrichmentDto mediaFileEnrichment) {

        MediaFile mediaFile = findOrCreate( // hydrate uuid because it is needed for the paths
                mediaFileRepository::findByMovieId,
                MediaFile::new,
                mediaFileEnrichment.movieId()
        );

        Movie movie = movieRepository.getReferenceById(mediaFileEnrichment.movieId());
        mediaFile.setContainerFormat(mediaFileEnrichment.containerFormat());
        mediaFile.setMovie(movie);
        mediaFile.setOriginalFilename(mediaFileEnrichment.originalFilename());
        mediaFile.setVideoCodec(mediaFileEnrichment.videoCodec());
        mediaFile.setAudioCodec(mediaFileEnrichment.audioCodec());
        mediaFile.setResolution(mediaFileEnrichment.resolution());
        mediaFile.setDurationSeconds(mediaFileEnrichment.durationSeconds());
        mediaFile.setFileSizeBytes(mediaFileEnrichment.fileSizeBytes());
        mediaFile.setEncodedAt(mediaFileEnrichment.encodedAt());
        mediaFile.setCreatedAt(Instant.now());
        mediaFileRepository.save(mediaFile);

        return mediaFile;
    }

    @Transactional
    public MediaFile upsertEpisodeMediaFile(CatalogEventDtos.MediaFileEnrichmentDto mediaFileEnrichment) {

        MediaFile mediaFile = findOrCreate( // hydrate uuid because it is needed for the paths
                mediaFileRepository::findByEpisodeId,
                MediaFile::new,
                mediaFileEnrichment.episodeId()
        );


        Episode episode = episodeRepository.getReferenceById(mediaFileEnrichment.episodeId());
        mediaFile.setContainerFormat(mediaFileEnrichment.containerFormat());
        mediaFile.setEpisode(episode);
        mediaFile.setOriginalFilename(mediaFileEnrichment.originalFilename());
        mediaFile.setVideoCodec(mediaFileEnrichment.videoCodec());
        mediaFile.setAudioCodec(mediaFileEnrichment.audioCodec());
        mediaFile.setResolution(mediaFileEnrichment.resolution());
        mediaFile.setDurationSeconds(mediaFileEnrichment.durationSeconds());
        mediaFile.setFileSizeBytes(mediaFileEnrichment.fileSizeBytes());
        mediaFile.setEncodedAt(mediaFileEnrichment.encodedAt());
        mediaFile.setCreatedAt(Instant.now());
        mediaFileRepository.save(mediaFile);

        return mediaFile;
    }

    /**
     * Creates or updates the Subtitle row for a subtitle file that
     * upload-service has finalized (converted + moved into the library).
     *
     * Uses the same findOrCreate-by-parent-id pattern as
     * upsertMovieMediaFile/upsertEpisodeMediaFile so this is safe to run
     * regardless of whether the corresponding MediaFile enrichment event
     * (sent on a different Kafka topic, with no ordering guarantee relative
     * to this one) has already been processed - whichever event arrives
     * first creates the MediaFile row, the other one just fills in more of
     * it.
     */
    @Transactional
    public Subtitle upsertSubtitle(CatalogEventDtos.SubtitleFinalizedDto dto) {

        MediaFile mediaFile;

        if (dto.movieId() != null) {
            mediaFile = findOrCreate(mediaFileRepository::findByMovieId, MediaFile::new, dto.movieId());
            mediaFile.setMovie(movieRepository.getReferenceById(dto.movieId()));
        } else if (dto.episodeId() != null) {
            mediaFile = findOrCreate(mediaFileRepository::findByEpisodeId, MediaFile::new, dto.episodeId());
            mediaFile.setEpisode(episodeRepository.getReferenceById(dto.episodeId()));
        } else {
            throw new IllegalArgumentException("SubtitleFinalizedDto must set either movieId or episodeId");
        }

        mediaFileRepository.save(mediaFile);

        Subtitle subtitle = subtitleRepository.findByFilePath(dto.filePath()).orElseGet(Subtitle::new);
        subtitle.setMediaFile(mediaFile);
        subtitle.setFilePath(dto.filePath());
        subtitle.setLanguageCode(dto.languageCode());
        subtitle.setLabel(dto.label());
        subtitle.setForced(dto.forced());
        subtitle.setSdh(dto.sdh());
        subtitle.setSource(dto.source());

        if (dto.format() != null && !dto.format().isBlank()) {
            subtitle.setFormat(dto.format());
        }

        return subtitleRepository.save(subtitle);
    }


    @Transactional
    public void updateFilePaths(CatalogEventDtos.CatalogPathUpdateDto catalogPathUpdateDto) {

        for (Map.Entry<UUID, String> mediaFile : catalogPathUpdateDto.mediaFilePaths().entrySet()) {
            UUID key = mediaFile.getKey();
            String path = mediaFile.getValue();

            System.out.println("mediafile: " + key + " " + path );

            mediaFileRepository.updateFilePathByMovieOrEpisodeId(key, path);
        }

        for (Map.Entry<UUID, String> episodeStill : catalogPathUpdateDto.episodeStillPaths().entrySet()) {
            UUID key = episodeStill.getKey();
            String path = episodeStill.getValue();

            episodeRepository.updateStillPath(key, path);
        }
        for (Map.Entry<UUID, String> seasonPoster : catalogPathUpdateDto.seasonPosterPaths().entrySet()) {
            UUID key = seasonPoster.getKey();
            String path = seasonPoster.getValue();

            seasonRepository.updatePosterPath(key, path);
        }

        for (Map.Entry<UUID, String> showPoster : catalogPathUpdateDto.showPosterPaths().entrySet()) {
            UUID key = showPoster.getKey();
            String path = showPoster.getValue();

            showRepository.updatePosterPath(key, path);
        }

        for (Map.Entry<UUID, String> showBackdrop : catalogPathUpdateDto.showBackdropPaths().entrySet()) {
            UUID key = showBackdrop.getKey();
            String path = showBackdrop.getValue();

            showRepository.updateBackdropPath(key, path);
        }

        for (Map.Entry<UUID, String> moviePoster : catalogPathUpdateDto.moviePosterPaths().entrySet()) {
            UUID key = moviePoster.getKey();
            String path = moviePoster.getValue();

            movieRepository.updatePosterPath(key, path);
        }

        for (Map.Entry<UUID, String> movieBackdrop : catalogPathUpdateDto.movieBackdropPaths().entrySet()) {
            UUID key = movieBackdrop.getKey();
            String path = movieBackdrop.getValue();

            movieRepository.updateBackdropPath(key, path);
        }

        if (catalogPathUpdateDto.subtitles() != null) {
            for (CatalogEventDtos.SubtitleFinalizedDto subtitleDto : catalogPathUpdateDto.subtitles()) {
                upsertSubtitle(subtitleDto);
            }
        }

    }
    @Transactional
    public Show upsertShow(FileUploadMetadata.ShowUploadMetadataDto metadata) {
        validateShow(metadata);

        Show show = findOrCreate(
                showRepository::findByTmdbId,
                Show::new,
                metadata.tmdbId()
        );
        show.setTmdbId(metadata.tmdbId());
        show.setTitle(metadata.title());
        show.setFirstAirDate(metadata.firstAirDate());
        show.setOverview(metadata.overview());
        show.setCreatorNames(metadata.creatorNames());
        show.setStatus(metadata.status());
        show.setTmdbPosterPath(metadata.posterPath());
        show.setTmdbBackdropPath(metadata.backdropPath());
        syncGenres(show, metadata.genres());
        show = saveWithRaceRetry(showRepository, show, showRepository::findByTmdbId, metadata.tmdbId());


        // collect the actual persisted Season entities instead of relying on show.getSeasons()
        List<Season> seasons = new ArrayList<>(metadata.seasons().size());
        for (FileUploadMetadata.SeasonUploadMetadataDto seasonDto : metadata.seasons()) {
            seasons.add(upsertSeason(seasonDto, show));
        }
        // sync the inmemory association so callers (and toShowResult) see the current state
        show.getSeasons().clear();
        show.getSeasons().addAll(seasons);

        return show;
    }

    private Season upsertSeason(FileUploadMetadata.SeasonUploadMetadataDto metadata, Show show) {
        Season season = findOrCreate(
                seasonRepository::findByTmdbId,
                Season::new,
                metadata.tmdbId()
        );
        season.setTmdbId(metadata.tmdbId());
        season.setShow(show);
        season.setSeasonNumber(metadata.seasonNumber());
        season.setTitle(metadata.title());
        season.setOverview(metadata.overview());
        season.setAirDate(metadata.airDate());
        season.setTmdbPosterPath(metadata.posterPath());

        season = saveWithRaceRetry(seasonRepository, season, seasonRepository::findByTmdbId, metadata.tmdbId());

        // same pattern one level down for episodes
        List<Episode> episodes = new ArrayList<>(metadata.episodes().size());
        for (FileUploadMetadata.EpisodeUploadMetadataDto episodeDto : metadata.episodes()) {
            episodes.add(upsertEpisode(episodeDto, season));
        }
        season.getEpisodes().clear();
        season.getEpisodes().addAll(episodes);

        return season;
    }

    private Episode upsertEpisode(FileUploadMetadata.EpisodeUploadMetadataDto metadata, Season season) {
        Episode episode = findOrCreate(
                episodeRepository::findByTmdbId,
                Episode::new,
                metadata.tmdbId()
        );

        episode.setTmdbId(metadata.tmdbId());
        episode.setSeason(season);
        episode.setTitle(metadata.title());
        episode.setAirDate(metadata.airDate());
        episode.setOverview(metadata.overview());
        episode.setRuntimeMinutes(metadata.runtimeMinutes());
        episode.setEpisodeNumber(metadata.episodeNumber());
        episode.setTmdbStillPath(metadata.stillPath());

        return saveWithRaceRetry(episodeRepository, episode, episodeRepository::findByTmdbId, metadata.tmdbId());
    }

    //whole metadata is validated up front and the upsert runs in one @transactional method
    // so a bad episode 4 rolls back episodes 1-3 too rather than leaving a halfcatalogued show

    private void validateMovie(FileUploadMetadata.MovieUploadMetadataDto metadata) {
        require(metadata.tmdbId() > 0, "tmdbId must be a positive TMDB id");
        require(notBlank(metadata.title()), "title is required");
        require(metadata.runtimeMinutes() == null || metadata.runtimeMinutes() > 0,
                "runtimeMinutes must be positive when present");
    }

    private void validateShow(FileUploadMetadata.ShowUploadMetadataDto metadata) {
        require(metadata.tmdbId() > 0, "tmdbId must be a positive TMDB id");
        require(notBlank(metadata.title()), "title is required");
        require(metadata.seasons() != null, "seasons list must not be null");
        require(!metadata.seasons().isEmpty(), "a show event must contain at least one season being uploaded");

        Set<Integer> seasonNumbers = new HashSet<>();
        Set<Long> seasonTmdbIds = new HashSet<>();
        for (FileUploadMetadata.SeasonUploadMetadataDto season : metadata.seasons()) {
            require(season.tmdbId() > 0, "season tmdbId must be positive");
            require(season.seasonNumber() != null && season.seasonNumber() >= 0,
                    "seasonNumber must be >= 0 (0 = specials)");
            require(seasonNumbers.add(season.seasonNumber()),
                    "duplicate seasonNumber " + season.seasonNumber() + " within the same event");
            require(seasonTmdbIds.add(season.tmdbId()),
                    "duplicate season tmdbId " + season.tmdbId() + " within the same event");
            validateSeason(season);
        }
    }

    private void validateSeason(FileUploadMetadata.SeasonUploadMetadataDto metadata) {
        require(metadata.episodes() != null, "episodes list must not be null");

        Set<Integer> episodeNumbers = new HashSet<>();
        Set<Long> episodeTmdbIds = new HashSet<>();
        for (FileUploadMetadata.EpisodeUploadMetadataDto episode : metadata.episodes()) {
            require(episode.tmdbId() > 0, "episode tmdbId must be positive");
            require(episode.episodeNumber() != null && episode.episodeNumber() >= 1,
                    "episodeNumber must be >= 1");
            require(episodeNumbers.add(episode.episodeNumber()),
                    "duplicate episodeNumber " + episode.episodeNumber() + " within season " + metadata.seasonNumber());
            require(episodeTmdbIds.add(episode.tmdbId()),
                    "duplicate episode tmdbId " + episode.tmdbId() + " within season " + metadata.seasonNumber());
            require(notBlank(episode.title()), "episode title is required (tmdbId " + episode.tmdbId() + ")");
        }
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new CatalogValidationException(message);
        }
    }

    // upsert by tmdb api

    private <T> T findOrCreate(Function<Long, Optional<T>> finder, Supplier<T> factory, long tmdbId) {
        return finder.apply(tmdbId).orElseGet(factory);
    }


    private <T> T findOrCreate(Function<UUID, Optional<T>> finder, Supplier<T> factory, UUID id) {
        return finder.apply(id).orElseGet(factory);
    }

    // upload jobs can race (especially with tv episodes, upserting the same show/ season)
    private <T> T saveWithRaceRetry(JpaRepository<T, UUID> repo,T entity,Function<Long, Optional<T>> finder,long tmdbId) {
        try {
            return repo.save(entity);
        } catch (DataIntegrityViolationException race) {
            return finder.apply(tmdbId).orElseThrow(() -> race);
        }
    }


    private void syncGenres(Show show, List<String> genreNames) {
        Set<String> desired = genreNames == null ? Set.of() : new HashSet<>(genreNames);

        // drop associations that are no longer wanted
        show.getGenres().removeIf(gtm -> !desired.contains(gtm.getGenre().getName()));

        Set<String> alreadyLinked = show.getGenres().stream()
                .map(gtm -> gtm.getGenre().getName())
                .collect(Collectors.toSet());

        for (String genreName : desired) {
            if (alreadyLinked.contains(genreName)) continue; // untouched, no delete+insert race

            Genre genre = genreService.findOrCreate(genreName);
            GenreToMedia relationship = new GenreToMedia();
            relationship.setGenre(genre);
            relationship.setShow(show);
            show.getGenres().add(relationship);
        }
    }

    private void syncGenres(Movie movie, List<String> genreNames) {
        Set<String> desired = genreNames == null ? Set.of() : new HashSet<>(genreNames);

        // drop associations that are no longer wanted
        movie.getGenres().removeIf(gtm -> !desired.contains(gtm.getGenre().getName()));

        Set<String> alreadyLinked = movie.getGenres().stream()
                .map(gtm -> gtm.getGenre().getName())
                .collect(Collectors.toSet());

        for (String genreName : desired) {
            if (alreadyLinked.contains(genreName)) continue; // untouched, no delete+insert race

            Genre genre = genreService.findOrCreate(genreName);
            GenreToMedia relationship = new GenreToMedia();
            relationship.setGenre(genre);
            relationship.setMovie(movie);
            movie.getGenres().add(relationship);
        }
    }
}