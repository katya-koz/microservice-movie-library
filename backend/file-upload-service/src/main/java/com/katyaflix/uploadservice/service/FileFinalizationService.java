package com.katyaflix.uploadservice.service;

import com.katyaflix.uploadservice.dto.CatalogEventDtos;
import com.katyaflix.uploadservice.dto.FileUploadMetadata;
import com.katyaflix.uploadservice.entity.UploadJob;
import com.katyaflix.uploadservice.messaging.CatalogTopics;
import com.katyaflix.uploadservice.util.SubtitleNaming;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FileFinalizationService {

    private static final Logger log =LoggerFactory.getLogger(FileFinalizationService.class);

    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final Path mediaRoot;
    private final Path tempMediaRoot;

    public FileFinalizationService(ObjectMapper objectMapper, KafkaTemplate<String, Object> kafkaTemplate, @Value("${media.root}") String mediaRoot
    ) {
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;

        this.mediaRoot = Path.of(mediaRoot).resolve("library");
        this.tempMediaRoot = Path.of(mediaRoot).resolve("temp");
    }

    @PostConstruct
    public void initSkeleton() throws IOException {
        for (Path p : List.of(
                mediaRoot.resolve("media").resolve("movies"),
                mediaRoot.resolve("media").resolve("shows"),
                mediaRoot.resolve("assets").resolve("movies"),
                mediaRoot.resolve("assets").resolve("shows"),
                tempMediaRoot
        )) {
            Files.createDirectories(p);
        }
    }
    private Path jobTempRoot(UploadJob job) {
        return tempMediaRoot.resolve("jobs").resolve(job.getId().toString());
    }
    public void finalizeMovie( UploadJob job
    ) throws IOException {

        FileUploadMetadata.MovieUploadMetadataDto movie = (FileUploadMetadata.MovieUploadMetadataDto) requirePayload(job);

        UUID movieId = movie.id();
        String title = movie.title();
        long tmdbId = movie.tmdbId();

        String camelCaseTitle = Arrays.stream(title.split("[^a-zA-Z0-9]+")).filter(s -> !s.isEmpty()).map(s -> Character.toUpperCase(s.charAt(0))+ s.substring(1).toLowerCase()).collect(Collectors.joining());
        Path jobRoot = jobTempRoot(job);
        Path tempMovieDir = jobRoot.resolve("movies").resolve(String.valueOf(tmdbId));
        Path tempAssetDir = jobRoot.resolve("assets").resolve("movies").resolve(String.valueOf(tmdbId));

//        Path tempMovieDir = tempMediaRoot.resolve("movies").resolve(String.valueOf(tmdbId));

        Path tempMovieFile = findSingleMediaFile(tempMovieDir);

//        Path tempAssetDir = tempMediaRoot.resolve("assets").resolve("movies").resolve(String.valueOf(tmdbId));

        Path finalMovieDir = mediaRoot.resolve("media").resolve("movies").resolve(movieId.toString());

        Path finalMovieAssetsDir = mediaRoot.resolve("assets").resolve("movies").resolve(movieId.toString());

        Files.createDirectories(finalMovieDir);
        Files.createDirectories(finalMovieAssetsDir);

        // finalize media

        Path finalMediaPath = finalMovieDir.resolve(camelCaseTitle + getExtension(tempMovieFile));

        moveMediaFile( tempMovieFile, finalMovieDir,camelCaseTitle);

        // finalize subtitles (uploaded alongside the video, saved into a
        // "subtitles" subdirectory so they don't confuse findSingleMediaFile)

        Path tempSubtitleDir = tempMovieDir.resolve("subtitles");
        Path finalSubtitleDir = finalMovieDir.resolve("subtitles");

        List<CatalogEventDtos.SubtitleFinalizedDto> subtitles = finalizeSubtitles(tempSubtitleDir, finalSubtitleDir, movieId, null);

        // finalize assets

        Path finalPosterPath = finalMovieAssetsDir.resolve("poster.jpg");
        Path finalBackdropPath = finalMovieAssetsDir.resolve("backdrop.jpg");
        boolean posterMoved = moveIfExists(tempAssetDir.resolve("poster.jpg"), finalPosterPath);
        boolean backdropMoved = moveIfExists(tempAssetDir.resolve("backdrop.jpg"), finalBackdropPath);



        // build path updated event (update catalog)

        Map<UUID, String> mediaFilePaths = new HashMap<>();
        mediaFilePaths.put(movieId, toRelativePath(finalMediaPath));

        Map<UUID, String> moviePosterPaths = new HashMap<>();
        if (posterMoved) {
            moviePosterPaths.put(movieId, toRelativePath(finalPosterPath));
        }

        Map<UUID, String> movieBackdropPaths = new HashMap<>();
        if (backdropMoved) {
            movieBackdropPaths.put(movieId, toRelativePath(finalBackdropPath));
        }

        CatalogEventDtos.CatalogPathUpdateDto pathUpdates =
                new CatalogEventDtos.CatalogPathUpdateDto(
                        mediaFilePaths,
                        Map.of(), // episodeStillPaths
                        Map.of(), // seasonPosterPaths
                        Map.of(), // showPosterPaths
                        Map.of(), // showBackdropPaths
                        moviePosterPaths,
                        movieBackdropPaths,
                        subtitles
                );

        sendPathUpdateEvent(job.getId(), pathUpdates);

        // cleanup the directories

        cleanupDirectory(tempAssetDir);
        cleanupDirectory(tempMovieDir);
        cleanupDirectory(jobRoot);
        log.info("Finalized movie upload. job={}, tmdbId={}, catalogId={}, subtitles={}", job.getId(), tmdbId, movieId, subtitles.size());
    }


    public void finalizeShow(UploadJob job
    ) throws IOException {

        FileUploadMetadata.ShowUploadMetadataDto show =
                (FileUploadMetadata.ShowUploadMetadataDto) requirePayload(job);

        UUID showId = show.id();
        long showTmdbId = show.tmdbId();
        Path jobRoot = jobTempRoot(job);
        Path tempShowDir = jobRoot.resolve("shows").resolve(String.valueOf(showTmdbId));
        Path tempAssetDir = jobRoot.resolve("assets").resolve("shows").resolve(String.valueOf(showTmdbId));
//        Path tempShowDir = tempMediaRoot.resolve("shows").resolve(String.valueOf(showTmdbId));
//        Path tempAssetDir = tempMediaRoot.resolve("assets").resolve("shows").resolve(String.valueOf(showTmdbId));

        Path finalShowDir = mediaRoot.resolve("media").resolve("shows").resolve(showId.toString());

        Path finalShowAssetsDir = mediaRoot.resolve("assets").resolve("shows").resolve(showId.toString());

        Files.createDirectories(finalShowDir);
        Files.createDirectories(finalShowAssetsDir);

        // these maps will be sent to catalog service for updating paths

        Map<UUID, String> mediaFilePaths = new HashMap<>();
        Map<UUID, String> episodeStillPaths = new HashMap<>();
        Map<UUID, String> seasonPosterPaths = new HashMap<>();
        Map<UUID, String> showPosterPaths = new HashMap<>();
        Map<UUID, String> showBackdropPaths = new HashMap<>();
        List<CatalogEventDtos.SubtitleFinalizedDto> subtitles = new ArrayList<>();

        //show assets

        Path finalShowPosterPath = finalShowAssetsDir.resolve("poster.jpg");

        Path finalShowBackdropPath = finalShowAssetsDir.resolve("backdrop.jpg");

        boolean showPosterMoved = moveIfExists(tempAssetDir.resolve("poster.jpg"), finalShowPosterPath);

        boolean showBackdropMoved = moveIfExists(tempAssetDir.resolve("backdrop.jpg"), finalShowBackdropPath);

        if (showPosterMoved) {
            showPosterPaths.put(showId,toRelativePath(finalShowPosterPath));
        }

        if (showBackdropMoved) {
            showBackdropPaths.put(showId,toRelativePath(finalShowBackdropPath));
        }

        //seasons and episodes

        for (FileUploadMetadata.SeasonUploadMetadataDto season : show.seasons()) {
            finalizeSeason(season, tempShowDir, tempAssetDir, finalShowDir, finalShowAssetsDir, mediaFilePaths, episodeStillPaths, seasonPosterPaths, subtitles);
        }

        // build and send path updates
        CatalogEventDtos.CatalogPathUpdateDto pathUpdates =
                new CatalogEventDtos.CatalogPathUpdateDto(
                        mediaFilePaths,
                        episodeStillPaths,
                        seasonPosterPaths,
                        showPosterPaths,
                        showBackdropPaths,
                        Map.of(), // moviePosterPaths
                        Map.of(),  // movieBackdropPaths
                        subtitles
                );

        sendPathUpdateEvent(job.getId(), pathUpdates);

        //cleanup
        cleanupDirectory(tempAssetDir);
        cleanupDirectory(tempShowDir);
        cleanupDirectory(jobRoot);

        log.info("Finalized show upload. job={}, tmdbId={}, catalogId={}, subtitles={}", job.getId(), showTmdbId, showId, subtitles.size());
    }


    private void finalizeSeason(
            FileUploadMetadata.SeasonUploadMetadataDto season,
            Path tempShowDir,
            Path tempAssetDir,
            Path finalShowDir,
            Path finalShowAssetsDir,
            Map<UUID, String> mediaFilePaths,
            Map<UUID, String> episodeStillPaths,
            Map<UUID, String> seasonPosterPaths,
            List<CatalogEventDtos.SubtitleFinalizedDto> subtitles
    ) throws IOException {

        UUID seasonId = season.id();
        long seasonTmdbId = season.tmdbId();

        Path finalSeasonDir = finalShowDir.resolve("seasons").resolve(seasonId.toString());

        Path finalSeasonAssetsDir = finalShowAssetsDir.resolve("seasons").resolve(seasonId.toString());

        Files.createDirectories(finalSeasonDir);
        Files.createDirectories(finalSeasonAssetsDir);

        //seasons poster

        Path finalSeasonPosterPath = finalSeasonAssetsDir.resolve("poster.jpg");

        boolean seasonPosterMoved = moveIfExists(tempAssetDir.resolve("seasons").resolve(String.valueOf(seasonTmdbId)).resolve("poster.jpg"), finalSeasonPosterPath);

        if (seasonPosterMoved) {
            seasonPosterPaths.put(seasonId, toRelativePath(finalSeasonPosterPath));
        }

        // episodes

        for (FileUploadMetadata.EpisodeUploadMetadataDto episode : season.episodes()) {
            finalizeEpisode(
                    episode,
                    tempShowDir,
                    tempAssetDir,
                    finalSeasonDir,
                    finalSeasonAssetsDir,
                    seasonTmdbId,
                    mediaFilePaths,
                    episodeStillPaths,
                    subtitles
            );
        }
    }


    private void finalizeEpisode(
            FileUploadMetadata.EpisodeUploadMetadataDto episode,
            Path tempShowDir,
            Path tempAssetDir,
            Path finalSeasonDir,
            Path finalSeasonAssetsDir,
            long seasonTmdbId,
            Map<UUID, String> mediaFilePaths,
            Map<UUID, String> episodeStillPaths,
            List<CatalogEventDtos.SubtitleFinalizedDto> subtitles
    ) throws IOException {

        UUID episodeId = episode.id();
        long episodeTmdbId = episode.tmdbId();

        Path finalEpisodeDir = finalSeasonDir.resolve("episodes").resolve(episodeId.toString());

        Path finalEpisodeAssetsDir = finalSeasonAssetsDir.resolve("episodes").resolve(episodeId.toString());

        Files.createDirectories(finalEpisodeDir);
        Files.createDirectories(finalEpisodeAssetsDir);

        // epsiode still

        Path finalStillPath = finalEpisodeAssetsDir.resolve("still.jpg");

        boolean stillMoved = moveIfExists(tempAssetDir.resolve("seasons").resolve(String.valueOf(seasonTmdbId)).resolve("episodes").resolve(String.valueOf(episodeTmdbId)).resolve("still.jpg"),
                finalStillPath);

        if (stillMoved) {
            episodeStillPaths.put(episodeId,toRelativePath(finalStillPath));
        }

        // epsiode mdia

        Path tempEpisodeDir = tempShowDir.resolve("seasons").resolve(String.valueOf(seasonTmdbId)).resolve("episodes").resolve(String.valueOf(episodeTmdbId));

        if (!Files.exists(tempEpisodeDir)) {
            throw new IOException("Temporary episode directory does not exist: " + tempEpisodeDir);
        }

        Path mediaFile = findSingleMediaFile(tempEpisodeDir);

        String extension = getExtension(mediaFile);

        Path finalMediaPath = finalEpisodeDir.resolve(episodeId + extension);

        moveMediaFile(mediaFile, finalEpisodeDir, episodeId.toString());

        mediaFilePaths.put(episodeId, toRelativePath(finalMediaPath));

        // episode subtitles

        Path tempSubtitleDir = tempEpisodeDir.resolve("subtitles");
        Path finalSubtitleDir = finalEpisodeDir.resolve("subtitles");

        subtitles.addAll(finalizeSubtitles(tempSubtitleDir, finalSubtitleDir, null, episodeId));
    }


    /**
     * Moves every regular file sitting in tempSubtitleDir (already converted
     * to .vtt by FileEncodingService#encodeSubtitle) into finalSubtitleDir,
     * renaming it to a random UUID filename, and returns a DTO per file
     * describing where it ended up so catalog-service can create the
     * corresponding Subtitle row. Safe to call when tempSubtitleDir doesn't
     * exist (i.e. no subtitles were uploaded) - returns an empty list.
     */
    private List<CatalogEventDtos.SubtitleFinalizedDto> finalizeSubtitles(
            Path tempSubtitleDir,
            Path finalSubtitleDir,
            UUID movieId,
            UUID episodeId
    ) throws IOException {

        if (!Files.exists(tempSubtitleDir)) {
            return List.of();
        }

        List<CatalogEventDtos.SubtitleFinalizedDto> results = new ArrayList<>();

        try (var stream = Files.list(tempSubtitleDir)) {
            List<Path> subtitleFiles = stream.filter(Files::isRegularFile).toList();

            for (Path source : subtitleFiles) {
                String originalFilename = source.getFileName().toString();
                SubtitleNaming.SubtitleNameInfo info = SubtitleNaming.parse(originalFilename);

                String finalFilename = UUID.randomUUID() + ".vtt";
                Path destination = finalSubtitleDir.resolve(finalFilename);

                move(source, destination);

                results.add(new CatalogEventDtos.SubtitleFinalizedDto(
                        movieId,
                        episodeId,
                        toRelativePath(destination),
                        info.languageCode(),
                        info.label(),
                        "vtt",
                        info.forced(),
                        info.sdh(),
                        // standalone sidecar file uploaded by the user, not
                        // extracted from the video container
                        "external"
                ));
            }
        }

        cleanupDirectory(tempSubtitleDir);

        return results;
    }


    private void moveMediaFile(Path source, Path destinationDirectory, String uuidName) throws IOException {

        if (!Files.exists(source)) {
            throw new IOException("Media file does not exist: " + source);
        }

        String extension = getExtension(source);

        Path destination = destinationDirectory.resolve(uuidName + extension);

        Files.createDirectories(destinationDirectory);

        move(source, destination);
    }


    private boolean moveIfExists(Path source, Path destination) throws IOException {

        if (!Files.exists(source)) {
            return false;
        }

        Files.createDirectories(destination.getParent());

        move(source, destination);

        return true;
    }


    private void move(Path source, Path destination) throws IOException {

        Files.createDirectories(destination.getParent());

        try {
            Files.move(source, destination, java.nio.file.StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (java.nio.file.AtomicMoveNotSupportedException e) {
            Files.move(source, destination);
        }
    }


    private Path findSingleMediaFile(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            throw new IOException("Temporary media directory does not exist: " + directory);
        }

        try (var stream = Files.list(directory)) {
            // note: this only lists REGULAR files, so the "subtitles"
            // subdirectory created alongside the video file is naturally
            // skipped here and never confused for a second media file.
            var files = stream.filter(Files::isRegularFile).toList();

            if (files.isEmpty()) {
                throw new IOException("No media file found in: " + directory);
            }

            if (files.size() > 1) {
                throw new IOException("Expected one media file in " + directory + " but found " + files.size());
            }

            return files.getFirst();
        }
    }


    private FileUploadMetadata.UploadMetadataDto requirePayload(UploadJob job) {

        FileUploadMetadata.UploadMetadataDto payload = job.getCatalogPayload();

        if (payload == null) {
            throw new IllegalStateException("Upload job " + job.getId() + " has no catalog payload");
        }

        return payload;
    }


    /**
     * converts an absolute filesystem path under mediaRoot into
     * the path exposed to catalog-service / nginx.
     */
    private String toRelativePath(Path path) {

        Path relative = mediaRoot.relativize(path);
        return "/" + relative.toString().replace('\\', '/');
    }


    private String getExtension(Path path) {

        String filename = path.getFileName().toString();
        int dot = filename.lastIndexOf('.');

        if (dot < 0) {
            return "";
        }

        return filename.substring(dot);
    }


    private void sendPathUpdateEvent(UUID fileUploadId, CatalogEventDtos.CatalogPathUpdateDto pathUpdates) {

        CatalogEventDtos.CatalogPathUpdateEvent event =
                new CatalogEventDtos.CatalogPathUpdateEvent(
                        fileUploadId,
                        java.time.Instant.now(),
                        pathUpdates
                );

        kafkaTemplate.send(CatalogTopics.FILE_PATH_UPDATE_REQUESTED, fileUploadId.toString(), event);

        log.info("Sent file path update event. fileUploadId={}", fileUploadId);
    }


    private void cleanupDirectory(Path directory) throws IOException {

        if (!Files.exists(directory)) {
            return;
        }

        try (var stream = Files.walk(directory)) {
            var paths = stream.sorted(java.util.Comparator.reverseOrder()).toList();

            for (Path path : paths) {
                Files.deleteIfExists(path);
            }
        }
    }
}