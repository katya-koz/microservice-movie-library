package com.katyaflix.uploadservice.messaging;


import com.katyaflix.uploadservice.dto.metadata.MovieMetadata;
import com.katyaflix.uploadservice.dto.metadata.ShowMetadata;
import com.katyaflix.uploadservice.dto.validationrequest.MovieCatalogValidationEvent;
import com.katyaflix.uploadservice.dto.validationrequest.ShowCatalogValidationEvent;
import com.katyaflix.uploadservice.entity.UploadJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class CatalogValidationProducer {

    private static final Logger log = LoggerFactory.getLogger(CatalogValidationProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public CatalogValidationProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishMovieValidation(UploadJob job, MovieMetadata dto) {
        MovieCatalogValidationEvent event =
                new MovieCatalogValidationEvent(job.getId(), Instant.now(), dto);
        send(CatalogTopics.MOVIE_VALIDATION_REQUESTED, job, event);
    }

    public void publishShowValidation(UploadJob job, ShowMetadata dto) {
        ShowCatalogValidationEvent event =
                new ShowCatalogValidationEvent(job.getId(), Instant.now(), dto);
        send(CatalogTopics.SHOW_VALIDATION_REQUESTED, job, event);
    }


    private void send(String topic, UploadJob job, Object event) {
        kafkaTemplate.send(topic, job.getId().toString(), event).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish catalog validation event to {} for job {}",topic, job.getId(), ex);
            }
        });
    }
}
