package com.katyaflix.uploadservice.messaging;

import com.katyaflix.uploadservice.dto.CatalogEventDtos;
import com.katyaflix.uploadservice.repository.UploadJobRepository;
import com.katyaflix.uploadservice.service.UploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class CatalogUploadResultListener {

    private static final Logger log =LoggerFactory.getLogger(CatalogUploadResultListener.class);

    private final UploadService uploadService;
    private final UploadJobRepository uploadJobRepository;

    public CatalogUploadResultListener(UploadJobRepository uploadJobRepository,UploadService uploadService) {
        this.uploadJobRepository = uploadJobRepository;
        this.uploadService = uploadService;
    }

    @KafkaListener(
            topics = CatalogTopics.MOVIE_VALIDATION_COMPLETED,
            groupId = "upload-service",
            containerFactory = "movieResultContainerFactory"
    )
    public void onMovieResult(CatalogEventDtos.CatalogMovieUploadEvent event) {

        uploadJobRepository.findById(event.fileUploadId()).ifPresentOrElse(job -> {
            if (event.status() ==  CatalogEventDtos.CatalogUpdateStatus.SUCCESS) {
                uploadJobRepository.updatePayloadWhereId(job.getId(),event.metadata(), Instant.now());
                uploadJobRepository.updateCatalogValidationStatusWhereId(job.getId(), true,Instant.now());
                uploadService.advance(job.getId());

            } else {
                uploadService.fail( job, event.message());
            }

        }, () -> log.warn( "Received movie catalog result for unknown upload job {}",event.fileUploadId()));
    }

    @KafkaListener(
            topics = CatalogTopics.SHOW_VALIDATION_COMPLETED,
            groupId = "upload-service",
            containerFactory = "showResultContainerFactory"
    )
    public void onShowResult(CatalogEventDtos.CatalogShowUploadEvent event) {
        uploadJobRepository.findById(event.fileUploadId()).ifPresentOrElse(job -> {
            if (event.status() == CatalogEventDtos.CatalogUpdateStatus.SUCCESS) {

                uploadJobRepository.updatePayloadWhereId( job.getId(),event.metadata(),Instant.now());
                uploadJobRepository.updateCatalogValidationStatusWhereId( job.getId(), true ,Instant.now());
                uploadService.advance(job.getId());

            } else {
                uploadService.fail(job,  event.message() );
            }

        }, () -> log.warn("Received show catalog result for unknown upload job {}", event.fileUploadId()));
    }
}