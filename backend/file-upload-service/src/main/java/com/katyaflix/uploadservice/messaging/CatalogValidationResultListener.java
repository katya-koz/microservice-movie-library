package com.katyaflix.uploadservice.messaging;

import com.katyaflix.uploadservice.dto.enums.EventStatus;
import com.katyaflix.uploadservice.dto.enums.PipelineStatus;
import com.katyaflix.uploadservice.dto.metadata.MovieMetadata;
import com.katyaflix.uploadservice.dto.metadata.ShowMetadata;
import com.katyaflix.uploadservice.dto.validationrequest.MovieCatalogValidationResult;
import com.katyaflix.uploadservice.dto.validationrequest.ShowCatalogValidationResult;
import com.katyaflix.uploadservice.repository.FileUploadRepository;
import com.katyaflix.uploadservice.repository.UploadJobRepository;
import com.katyaflix.uploadservice.service.UploadService;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class CatalogValidationResultListener {

    private static final Logger log =LoggerFactory.getLogger(CatalogValidationResultListener.class);

    private final UploadService uploadService;
    private final UploadJobRepository uploadJobRepository;


    @Transactional
    @KafkaListener(
            topics = CatalogTopics.MOVIE_VALIDATION_COMPLETED,
            groupId = "upload-service"
    )
    public void onMovieResult(MovieCatalogValidationResult result) {
        uploadJobRepository.findById(result.fileUploadId()).ifPresentOrElse(job ->
        {
            if (result.status() ==  EventStatus.SUCCESS) {
                job.setMediaMetadata(result.metadata());
                job.setValidationStatus(PipelineStatus.COMPLETED);
                uploadJobRepository.save(job);

                try {
                    uploadService.downloadMovieAssets((MovieMetadata) job.getMediaMetadata());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }else {
//                uploadService.fail(job, result.message());
            }
        }, () -> log.warn( "Received movie catalog result for unknown file upload job {}", result.fileUploadId()));

    }

    @Transactional
    @KafkaListener(
            topics = CatalogTopics.SHOW_VALIDATION_COMPLETED,
            groupId = "upload-service"
    )
    public void onShowResult(ShowCatalogValidationResult result) {
        uploadJobRepository.findById(result.fileUploadId()).ifPresentOrElse(job ->
        {
            if (result.status() ==  EventStatus.SUCCESS) {
                job.setMediaMetadata(result.metadata());
                job.setValidationStatus(PipelineStatus.COMPLETED);
                uploadJobRepository.save(job);

                try {
                    uploadService.downloadShowAssets((ShowMetadata) job.getMediaMetadata());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }else {
//                uploadService.fail(job, result.message());
            }
        }, () -> log.warn( "Received show catalog result for unknown file upload job {}", result.fileUploadId()));
    }
}