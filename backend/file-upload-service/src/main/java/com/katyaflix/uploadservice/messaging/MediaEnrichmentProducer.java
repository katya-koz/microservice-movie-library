package com.katyaflix.uploadservice.messaging;


import com.katyaflix.uploadservice.dto.enums.MediaCategory;
import com.katyaflix.uploadservice.dto.mediaenrichment.MediaEnrichment;
import com.katyaflix.uploadservice.dto.mediaenrichment.MediaEnrichmentEvent;
import com.katyaflix.uploadservice.dto.mediaenrichment.SubtitleEnrichment;
import com.katyaflix.uploadservice.dto.mediaenrichment.SubtitleEnrichmentEvent;
import com.katyaflix.uploadservice.dto.metadata.MovieMetadata;
import com.katyaflix.uploadservice.dto.metadata.ShowMetadata;
import com.katyaflix.uploadservice.dto.validationrequest.MovieCatalogValidationEvent;
import com.katyaflix.uploadservice.dto.validationrequest.ShowCatalogValidationEvent;
import com.katyaflix.uploadservice.entity.FileUpload;
import com.katyaflix.uploadservice.entity.UploadJob;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaEnrichmentProducer {

//    private static final Logger log = LoggerFactory.getLogger(CatalogValidationProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishMediaEnrichment(FileUpload job, MediaCategory cat, MediaEnrichment mediaFile) {

        MediaEnrichmentEvent event =
                new MediaEnrichmentEvent(
                        job.getId(),
                        Instant.now(),
                        cat,
                        mediaFile
                );

        send(CatalogTopics.MEDIA_ENRICHMENT_REQUESTED,job, event);
    }

    public void publishSubtitleEnrichment(FileUpload job, MediaCategory cat, SubtitleEnrichment subFile) {
        SubtitleEnrichmentEvent event =
                new SubtitleEnrichmentEvent(
                        job.getId(),
                        Instant.now(),
                        cat,
                        subFile
                );

        send(CatalogTopics.SUBTITLE_ENRICHMENT_REQUESTED,job, event);
    }


    private void send(String topic, FileUpload job, Object event) {
        kafkaTemplate.send(topic, job.getId().toString(), event).whenComplete((result, ex) -> {
            if (ex != null) {
//                log.error("Failed to publish catalog validation event to {} for job {}",topic, job.getId(), ex);
            }
        });
    }
}
