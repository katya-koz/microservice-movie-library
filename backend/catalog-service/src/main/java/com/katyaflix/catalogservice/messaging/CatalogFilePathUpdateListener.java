package com.katyaflix.catalogservice.messaging;

import com.katyaflix.catalogservice.dto.CatalogEventDtos;
import com.katyaflix.catalogservice.service.CatalogUpsertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class CatalogFilePathUpdateListener {
    private static final Logger log = LoggerFactory.getLogger(CatalogFilePathUpdateListener.class);


    private final CatalogUpsertService upsertService;
    private final KafkaTemplate<String, Object> kafkaTemplate;


    public CatalogFilePathUpdateListener(CatalogUpsertService upsertService, KafkaTemplate<String, Object> kafkaTemplate) {
        this.upsertService = upsertService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(
            topics = CatalogTopics.FILE_PATH_UPDATE_REQUESTED,
            groupId = "catalog-service",
            containerFactory = "filePathUpdateContainerFactory"
    )
    public void onFilePathUpdate(CatalogEventDtos.CatalogPathUpdateEvent event) {
        CatalogEventDtos.FilePathUpdateEvent result;
        System.out.println("recieved FILE PATH request!");

        try {
            upsertService.updateFilePaths(event.pathUpdates());
            System.out.println("recieved FILE PATH request! --- SUCCESS");
            result = new CatalogEventDtos.FilePathUpdateEvent(event.fileUploadId(), Instant.now(), CatalogEventDtos.CatalogUpdateStatus.SUCCESS, "Successfully added media file enrichment.");
        } catch (Exception e) {
            System.out.println("recieved FILE PATH request! --- fail");
            log.warn("Rejected file path update event fileUploadId={}: {}",   event.fileUploadId(), e.getMessage());
            result = new CatalogEventDtos.FilePathUpdateEvent(event.fileUploadId(), Instant.now(), CatalogEventDtos.CatalogUpdateStatus.FAILURE, e.getMessage());
        }

        System.out.println("sending validation completed event");
        kafkaTemplate.send(CatalogTopics.FILE_PATH_UPDATE_COMPLETED, event.fileUploadId().toString(), result);
    }
}
