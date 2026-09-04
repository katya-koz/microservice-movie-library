package com.katyaflix.uploadservice.messaging;


import com.katyaflix.uploadservice.dto.CatalogEventDtos;
import com.katyaflix.uploadservice.dto.FileUploadMetadata.MovieUploadMetadataDto;
import com.katyaflix.uploadservice.dto.FileUploadMetadata.ShowUploadMetadataDto;
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

    public void publishMovieValidation(UploadJob job, MovieUploadMetadataDto dto) {
        CatalogEventDtos.MovieCatalogValidationEvent event = new CatalogEventDtos.MovieCatalogValidationEvent(job.getId(), Instant.now(), dto);
        send(CatalogTopics.MOVIE_VALIDATION_REQUESTED, job, event);
    }

    public void publishShowValidation(UploadJob job, ShowUploadMetadataDto dto) {
        CatalogEventDtos.ShowCatalogValidationEvent event = new CatalogEventDtos.ShowCatalogValidationEvent(job.getId(), Instant.now(), dto);
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
