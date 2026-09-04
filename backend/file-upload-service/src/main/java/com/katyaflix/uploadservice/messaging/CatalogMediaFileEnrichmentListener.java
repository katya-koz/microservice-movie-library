package com.katyaflix.uploadservice.messaging;

import com.katyaflix.uploadservice.dto.CatalogEventDtos;
import com.katyaflix.uploadservice.service.UploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;


@Component
public class CatalogMediaFileEnrichmentListener {

    private final UploadService uploadService;
    private static final Logger log = LoggerFactory.getLogger(CatalogUploadResultListener.class);


    public CatalogMediaFileEnrichmentListener(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @KafkaListener(
            topics = CatalogTopics.MEDIA_ENRICHMENT_COMPLETED,
            groupId = "catalog-service",
            containerFactory = "mediaEnrichmentContainerFactory"
    )
    public void onFilePathUpdate(CatalogEventDtos.MediaCatalogEnrichmentEvent event) {

        try {
            if (event.status() == CatalogEventDtos.CatalogUpdateStatus.SUCCESS) {
                uploadService.updateMediaEnrichmentStatusById(event.fileUploadId(), true);
            }
            else{
                uploadService.updateMediaEnrichmentStatusById(event.fileUploadId(), false);
            }

        }catch (Exception e) {
            log.warn("couldnt complete media enrichment fileUploadId={}: {}",   event.fileUploadId(), e.getMessage());
            uploadService.appendErrorMessageById(event.fileUploadId(), e.getMessage());

        }

    }
}

