package com.katyaflix.catalogservice.messaging;

import com.katyaflix.catalogservice.dto.CatalogEventDtos.CatalogMovieUploadEvent;
import com.katyaflix.catalogservice.dto.CatalogEventDtos.CatalogShowUploadEvent;
import com.katyaflix.catalogservice.dto.CatalogEventDtos.CatalogUpdateStatus;
import com.katyaflix.catalogservice.dto.CatalogEventDtos.MovieCatalogValidationEvent;
import com.katyaflix.catalogservice.dto.CatalogEventDtos.ShowCatalogValidationEvent;
import com.katyaflix.catalogservice.dto.FileUploadMetadata;
import com.katyaflix.catalogservice.entity.Episode;
import com.katyaflix.catalogservice.entity.Movie;
import com.katyaflix.catalogservice.entity.Season;
import com.katyaflix.catalogservice.entity.Show;
import com.katyaflix.catalogservice.service.CatalogUpsertService;
import com.katyaflix.catalogservice.service.CatalogValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Component
public class CatalogValidationListener {

    private static final Logger log = LoggerFactory.getLogger(CatalogValidationListener.class);

    private final CatalogUpsertService upsertService;
    private final KafkaTemplate<String, Object> kafkaTemplate;


    public CatalogValidationListener(CatalogUpsertService upsertService, KafkaTemplate<String, Object> kafkaTemplate) {
        this.upsertService = upsertService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(
            topics = CatalogTopics.MOVIE_VALIDATION_REQUESTED,
            groupId = "catalog-service",
            containerFactory = "movieValidationContainerFactory"
    )
    public void onMovieValidation(MovieCatalogValidationEvent event) {
        CatalogMovieUploadEvent result;
        try {
            FileUploadMetadata.MovieUploadMetadataDto metadata = event.metadata();
            Movie movie = upsertService.upsertMovie(metadata);
            result = new CatalogMovieUploadEvent(
                    new FileUploadMetadata.MovieUploadMetadataDto(
                            metadata.tmdbId(),
                            movie.getId(),
                            metadata.type(),
                            metadata.title(),
                            metadata.releaseDate(),
                            metadata.overview(),
                            metadata.creatorNames(),
                            metadata.runtimeMinutes(),
                            metadata.posterPath(),
                            metadata.backdropPath(),
                            metadata.files(),
                            metadata.genres()
                    ),
                    CatalogUpdateStatus.SUCCESS,
                    "Movie catalogued.",
                    LocalDate.now(),
                    event.fileUploadId()
            );
        } catch (CatalogValidationException e) {
            log.warn("Rejected movie validation event tmdbId={} fileUploadId={}: {}",  event.metadata().tmdbId(), event.fileUploadId(), e.getMessage());
            result = failedMovie(event, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error cataloguing movie tmdbId={} fileUploadId={}", event.metadata().tmdbId(), event.fileUploadId(), e);
            result = failedMovie(event, "Unexpected server error: " + e.getMessage());
        }

        System.out.println("sending validation completed event");
        kafkaTemplate.send(CatalogTopics.MOVIE_VALIDATION_COMPLETED, event.fileUploadId().toString(), result);
    }

    @KafkaListener(
            topics = CatalogTopics.SHOW_VALIDATION_REQUESTED,
            groupId = "catalog-service",
            containerFactory = "showValidationContainerFactory")
    public void onShowValidation(ShowCatalogValidationEvent event) {
        CatalogShowUploadEvent result;
        try {
            FileUploadMetadata.ShowUploadMetadataDto metadata = event.metadata();
            Show show = upsertService.upsertShow(metadata);
            result = toShowResult(show, event);
        } catch (CatalogValidationException e) {
            log.warn("Rejected show validation event tmdbId={} fileUploadId={}: {}", event.metadata().tmdbId(), event.fileUploadId(), e.getMessage());
            result = failedShow(event, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error cataloguing show tmdbId={} fileUploadId={}", event.metadata().tmdbId(), event.fileUploadId(), e);
            result = failedShow(event, "Unexpected server error: " + e.getMessage());
        }

        kafkaTemplate.send(CatalogTopics.SHOW_VALIDATION_COMPLETED, event.fileUploadId().toString(), result);
    }



    private CatalogShowUploadEvent toShowResult(
            Show show,
            ShowCatalogValidationEvent event
    ) {
        FileUploadMetadata.ShowUploadMetadataDto original = event.metadata();

        List<FileUploadMetadata.SeasonUploadMetadataDto> seasonResults = original.seasons().stream() .map(inputSeason -> {
                    Season season = show.getSeasons().stream()
                            .filter(s -> Objects.equals(
                                    s.getTmdbId(),
                                    inputSeason.tmdbId()
                            ))
                            .findFirst()
                            .orElseThrow();

                    List<FileUploadMetadata.EpisodeUploadMetadataDto> episodeResults =
                            inputSeason.episodes().stream()
                                    .map(inputEpisode -> {

                                        Episode episode =
                                                season.getEpisodes().stream()
                                                        .filter(e -> Objects.equals(
                                                                e.getTmdbId(),
                                                                inputEpisode.tmdbId()
                                                        ))
                                                        .findFirst()
                                                        .orElseThrow();

                                        return new FileUploadMetadata.EpisodeUploadMetadataDto(
                                                episode.getTmdbId(),
                                                episode.getId(),
                                                episode.getTitle(),
                                                episode.getAirDate(),
                                                episode.getOverview(),
                                                "",
                                                episode.getRuntimeMinutes(),
                                                episode.getEpisodeNumber(),
                                                episode.getStillPath(),

                                                // IMPORTANT:
                                                // files came from the upload event
                                                inputEpisode.files()
                                        );
                                    })
                                    .toList();

                    return new FileUploadMetadata.SeasonUploadMetadataDto(
                            season.getTmdbId(),
                            season.getId(),
                            season.getSeasonNumber(),
                            season.getTitle(),
                            season.getOverview(),
                            season.getAirDate(),
                            season.getPosterPath(),
                            episodeResults
                    );
                })
                .toList();

        FileUploadMetadata.ShowUploadMetadataDto showUpload =
                new FileUploadMetadata.ShowUploadMetadataDto(
                        show.getTmdbId(),
                        show.getId(),
                        FileUploadMetadata.UploadType.SHOW,
                        show.getTitle(),
                        show.getFirstAirDate(),
                        show.getOverview(),
                        show.getCreatorNames(),
                        show.getStatus(),
                        show.getPosterPath(),
                        show.getBackdropPath(),
                        seasonResults,
                        original.genres()
                );

        return new CatalogShowUploadEvent(
                showUpload,
                CatalogUpdateStatus.SUCCESS,
                "Show catalogued.",
                LocalDate.now(),
                event.fileUploadId()
        );
    }

    private CatalogMovieUploadEvent failedMovie(MovieCatalogValidationEvent event, String message) {
        return new CatalogMovieUploadEvent(event.metadata(), CatalogUpdateStatus.FAILURE, message, null, event.fileUploadId());
    }

    private CatalogShowUploadEvent failedShow(ShowCatalogValidationEvent event, String message) {
        return new CatalogShowUploadEvent(event.metadata(),CatalogUpdateStatus.FAILURE ,message, LocalDate.now(), event.fileUploadId());
    }
}
