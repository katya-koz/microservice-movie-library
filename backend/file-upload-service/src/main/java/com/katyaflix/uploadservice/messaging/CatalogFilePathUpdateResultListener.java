package com.katyaflix.uploadservice.messaging;

import com.katyaflix.uploadservice.dto.CatalogEventDtos;
import com.katyaflix.uploadservice.service.UploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
@Component
public class CatalogFilePathUpdateResultListener {

    private final UploadService uploadService;
    private static final Logger log = LoggerFactory.getLogger(CatalogUploadResultListener.class);


    public CatalogFilePathUpdateResultListener(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @KafkaListener(
            topics = CatalogTopics.FILE_PATH_UPDATE_COMPLETED,
            groupId = "catalog-service",
            containerFactory = "filePathUpdateContainerFactory"
    )
    public void onFilePathUpdate(CatalogEventDtos.FilePathUpdateEvent event) {

        try {
            if (event.status() == CatalogEventDtos.CatalogUpdateStatus.SUCCESS) {
                uploadService.completeFileUpload(event.fileUploadId()); // this is the last step ever... so it sets its own status to true, and filepatupdatestatus to true on complete
            }
            else{
                uploadService.appendErrorMessageById(event.fileUploadId(), event.message());
            }

    }catch (Exception e) {
            log.warn("couldnt finalzie file paths fileUploadId={}: {}",   event.fileUploadId(), e.getMessage());
            uploadService.appendErrorMessageById(event.fileUploadId(), e.getMessage());

           }

    }
}
