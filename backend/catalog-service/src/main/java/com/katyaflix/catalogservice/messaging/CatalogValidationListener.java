package com.katyaflix.catalogservice.messaging;

import com.katyaflix.catalogservice.dto.enums.EventStatus;
import com.katyaflix.catalogservice.dto.metadata.MovieMetadata;
import com.katyaflix.catalogservice.dto.metadata.ShowMetadata;
import com.katyaflix.catalogservice.dto.validationrequest.MovieCatalogValidationEvent;
import com.katyaflix.catalogservice.dto.validationrequest.MovieCatalogValidationResult;
import com.katyaflix.catalogservice.dto.validationrequest.ShowCatalogValidationEvent;
import com.katyaflix.catalogservice.dto.validationrequest.ShowCatalogValidationResult;
import com.katyaflix.catalogservice.entity.Movie;
import com.katyaflix.catalogservice.entity.Show;
import com.katyaflix.catalogservice.service.CatalogValidationException;
import com.katyaflix.catalogservice.service.MovieService;
import com.katyaflix.catalogservice.service.ShowService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class CatalogValidationListener {

    private static final Logger log = LoggerFactory.getLogger(CatalogValidationListener.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MovieService movieService;
    private final ShowService showService;

    @KafkaListener(
            topics = CatalogTopics.MOVIE_VALIDATION_REQUESTED,
            groupId = "catalog-service",
            properties = "spring.json.value.default.type=com.katyaflix.catalogservice.dto.validationrequest.MovieCatalogValidationEvent"

    )
    public void onMovieValidation(MovieCatalogValidationEvent event) {
        MovieCatalogValidationResult result;
        try {
            MovieMetadata metadata = event.metadata();

            // initial upsert based on metadata from tmdb
            Movie movie = movieService.upsertMovie(metadata);

            //return with catalog id
            result = new MovieCatalogValidationResult
                    (
                        new MovieMetadata(movie),
                        EventStatus.SUCCESS,
                        "Movie successfully catalogued.",
                        LocalDate.now(),
                        event.uploadJobId()
                    );
        } catch (CatalogValidationException e) {
//            log.warn("Rejected movie validation event tmdbId={} uploadJobId={}: {}",  event.metadata().tmdbId(), event.uploadJobId(), e.getMessage());
            result = failedMovie(event, e.getMessage());
        } catch (Exception e) {
//            log.error("Unexpected error cataloguing movie tmdbId={} uploadJobId={}", event.metadata().tmdbId(), event.uploadJobId(), e);
            result = failedMovie(event, "Unexpected server error: " + e.getMessage());
        }

//        System.out.println("sending validation completed event");
        kafkaTemplate.send(CatalogTopics.MOVIE_VALIDATION_COMPLETED, event.uploadJobId().toString(), result);
    }

    @KafkaListener(
            topics = CatalogTopics.SHOW_VALIDATION_REQUESTED,
            groupId = "catalog-service",
            properties = "spring.json.value.default.type=com.katyaflix.catalogservice.dto.validationrequest.ShowCatalogValidationEvent"
    )
    public void onShowValidation(ShowCatalogValidationEvent event) {
        ShowCatalogValidationResult result;
        try {
            ShowMetadata metadata = event.metadata();
            Show show = showService.upsertShow(metadata);
            result = new ShowCatalogValidationResult
                    (
                            new ShowMetadata(show),
                            EventStatus.SUCCESS,
                            "Show successfully catalogued.",
                            LocalDate.now(),
                            event.uploadJobId()
                    );

        } catch (CatalogValidationException e) {
            log.warn("Rejected show validation event tmdbId={} uploadJobId={}: {}", event.metadata().tmdbId(), event.uploadJobId(), e.getMessage());
            result = failedShow(event, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error cataloguing show tmdbId={} uploadJobId={}", event.metadata().tmdbId(), event.uploadJobId(), e);
            result = failedShow(event, "Unexpected server error: " + e.getMessage());
        }

        kafkaTemplate.send(CatalogTopics.SHOW_VALIDATION_COMPLETED, event.uploadJobId().toString(), result);
    }


    private MovieCatalogValidationResult failedMovie(MovieCatalogValidationEvent event, String message) {
        return new MovieCatalogValidationResult(event.metadata(), EventStatus.FAIL, message, null, event.uploadJobId());
    }

    private ShowCatalogValidationResult failedShow(ShowCatalogValidationEvent event, String message) {
        return new ShowCatalogValidationResult(event.metadata(),EventStatus.FAIL ,message, LocalDate.now(), event.uploadJobId());
    }
}
