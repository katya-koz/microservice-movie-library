package com.katyaflix.catalogservice.messaging;

import com.katyaflix.catalogservice.dto.CatalogEventDtos;
import com.katyaflix.catalogservice.dto.CatalogEventDtos.CatalogUpdateStatus;
import com.katyaflix.catalogservice.service.CatalogUpsertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class CatalogEnrichmentListener {

    private static final Logger log = LoggerFactory.getLogger(CatalogEnrichmentListener.class);

    private final CatalogUpsertService upsertService;
    private final KafkaTemplate<String, Object> kafkaTemplate;


    public CatalogEnrichmentListener(CatalogUpsertService upsertService, KafkaTemplate<String, Object> kafkaTemplate) {
        this.upsertService = upsertService;
        this.kafkaTemplate = kafkaTemplate;
    }
    @KafkaListener(
            topics = CatalogTopics.MEDIA_ENRICHMENT_REQUESTED,
            groupId = "catalog-service",
            containerFactory = "mediaEnrichmentContainerFactory"
    )
    public void onMediaEnrichment(CatalogEventDtos.MediaFileEnrichmentEvent event) {
        CatalogEventDtos.MediaCatalogEnrichmentEvent result;
//        System.out.println("recieved enrichment request!");
        try {
            CatalogEventDtos.MediaFileEnrichmentDto mediaFile = event.mediaFile();
            if(mediaFile.movieId() != null) {
                upsertService.upsertMovieMediaFile(mediaFile);
            }else{
                upsertService.upsertEpisodeMediaFile(mediaFile);
            }

//            System.out.println("recieved enrichment request! --- SUCCESS");
            result = new CatalogEventDtos.MediaCatalogEnrichmentEvent(event.fileUploadId(), Instant.now(), CatalogUpdateStatus.SUCCESS, "Successfully added media file enrichment.");
        } catch (Exception e) {
//            System.out.println("recieved enrichment request! --- FAIL");
            log.warn("Rejected media enrichment event fileUploadId={}: {}",   event.fileUploadId(), e.getMessage());
            result = new CatalogEventDtos.MediaCatalogEnrichmentEvent(event.fileUploadId(), Instant.now(), CatalogUpdateStatus.FAILURE, e.getMessage());
        }

//        System.out.println("sending validation completed event");
        kafkaTemplate.send(CatalogTopics.MEDIA_ENRICHMENT_COMPLETED, event.fileUploadId().toString(), result);
    }
}
