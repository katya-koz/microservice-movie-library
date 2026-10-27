package com.katyaflix.catalogservice.messaging;


import com.katyaflix.catalogservice.dto.enums.EventStatus;
import com.katyaflix.catalogservice.dto.enums.MediaCategory;
import com.katyaflix.catalogservice.dto.mediaenrichment.*;
import com.katyaflix.catalogservice.service.MediaService;
import com.katyaflix.catalogservice.service.SubtitleService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@RequiredArgsConstructor
@Component
public class CatalogEnrichmentListener {

    private static final Logger log = LoggerFactory.getLogger(CatalogEnrichmentListener.class);

    private final MediaService mediaService;
    private final SubtitleService subtitleService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(
            topics = CatalogTopics.MEDIA_ENRICHMENT_REQUESTED,
            groupId = "catalog-service"
    )
    public void onMediaEnrichment(MediaEnrichmentEvent event) {
        MediaEnrichmentResult result;
        try {
            MediaEnrichment mediaFile = event.mediaFile();
            if(event.mediaType() == MediaCategory.MOVIE) {
                mediaService.upsertMovieFile(mediaFile);
            }else{
                mediaService.upsertEpisodeFile(mediaFile);
            }

//            System.out.println("recieved enrichment request! --- SUCCESS");
            result = new MediaEnrichmentResult(event.fileUploadId(), Instant.now(), EventStatus.SUCCESS, "Successfully added media file.");
        } catch (Exception e) {
//            System.out.println("recieved enrichment request! --- FAIL");
            log.warn("Rejected media enrichment event fileUploadId={}: {}",   event.fileUploadId(), e.getMessage());
            result = new MediaEnrichmentResult(event.fileUploadId(), Instant.now(), EventStatus.FAIL, e.getMessage());
        }

//        System.out.println("sending validation completed event");
        kafkaTemplate.send(CatalogTopics.MEDIA_ENRICHMENT_COMPLETED, event.fileUploadId().toString(), result);
    }



    @KafkaListener(
            topics = CatalogTopics.SUBTITLE_ENRICHMENT_REQUESTED,
            groupId = "catalog-service"
    )
    public void onSubtitleEnrichment(SubtitleEnrichmentEvent event) {
        SubtitleEnrichmentResult result;
        try {
            SubtitleEnrichment subtitle = event.subtitleEnrichment();
            subtitleService.upsertSubtitleFile(subtitle, event.mediaType());

            result = new SubtitleEnrichmentResult(event.fileUploadId(), Instant.now(), EventStatus.SUCCESS, "Successfully added subtitle file.");
        } catch (Exception e) {
//            System.out.println("recieved enrichment request! --- FAIL");
            log.warn("Rejected media enrichment event fileUploadId={}: {}",   event.fileUploadId(), e.getMessage());
            result = new SubtitleEnrichmentResult(event.fileUploadId(), Instant.now(), EventStatus.FAIL, e.getMessage());
        }

//        System.out.println("sending validation completed event");
        kafkaTemplate.send(CatalogTopics.SUBTITLE_ENRICHMENT_COMPLETED, event.fileUploadId().toString(), result);
    }
}
