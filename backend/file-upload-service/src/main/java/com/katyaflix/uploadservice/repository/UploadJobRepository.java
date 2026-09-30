package com.katyaflix.uploadservice.repository;

import com.katyaflix.uploadservice.dto.EncodedMediaFile;
import com.katyaflix.uploadservice.dto.FileUploadMetadata;
import com.katyaflix.uploadservice.entity.UploadJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;


public interface UploadJobRepository extends MongoRepository<UploadJob, UUID> {

    @Query("{ '_id' : ?0 }")
    @Update(pipeline = {
            "{ '$set': { 'status': ?1, 'updated_at': ?2 } }"
    })
    void updateStatusWhereId(
            UUID id,
            UploadJob.UploadStatus status,
            Instant updatedAt
    );


    @Query("{ '_id' : ?0 }")
    @Update(pipeline = {
            "{ '$set': { 'media_file_enrichment_status': ?1, 'updated_at': ?2 } }"
    })
    void updateMediaEnrichmentStatusWhereId(
            UUID id,
            boolean mediaEnrichmentStatus,
            Instant updatedAt
    );


    @Query("{ '_id' : ?0 }")
    @Update(pipeline = {
            "{ '$set': { 'file_path_update_status': ?1, 'updated_at': ?2 } }"
    })
    void updateFilePathUpdateStatusWhereId(
            UUID id,
            boolean filePathUpdateStatus,
            Instant updatedAt
    );


    @Query("{ '_id' : ?0 }")
    @Update(pipeline = {
            "{ '$set': { 'catalog_validation_status': ?1, 'updated_at': ?2 } }"
    })
    void updateCatalogValidationStatusWhereId(
            UUID id,
            boolean catalogValidationStatus,
            Instant updatedAt
    );


    @Query("{ '_id' : ?0 }")
    @Update(pipeline = {
            "{ '$set': { 'file_upload_status': ?1, 'updated_at': ?2 } }"
    })
    void updateFileUploadStatusWhereId(
            UUID id,
            boolean fileUploadStatus,
            Instant updatedAt
    );


    @Query("{ '_id' : ?0 }")
    @Update(pipeline = {
            "{ '$set': { 'catalog_payload': ?1, 'updated_at': ?2 } }"
    })
    void updatePayloadWhereId(
            UUID id,
            FileUploadMetadata.UploadMetadataDto payload,
            Instant updatedAt
    );


    /**
     * Add to errors list.
     */
    @Query("{ '_id' : ?0 }")
    @Update("{ '$push': { 'error_messages': ?1 }, '$set': { 'updated_at': ?2 } }")
    void appendErrorMessage(
            UUID id,
            String message,
            Instant updatedAt
    );


    @Query("{ '_id' : ?0 }")
    @Update(pipeline = {
            "{ '$set': { 'completed_at': ?1, 'updated_at': ?1 } }"
    })
    void updateCompletedAtWhereId(
            UUID jobId,
            Instant completedAt
    );


    @Query(value = "{ '_id': ?0 }", fields = "{ 'encoded_media_files': 1 }")
    Optional<UploadJob> findEncodedMediaFilesById(UUID id);


    @Query("{ '_id': ?0 }")
    @Update("""
    {
        "$set": {
            "encoded_media_files": ?1
        }
    }
    """)
    long saveEncodedMediaFiles(
            UUID id,
            List<EncodedMediaFile> encodedMediaFiles
    );


    Page<UploadJob> findAllByOrderByCreatedAtDesc(Pageable pageable);


    @Query("{ '_id' : ?0 }")
    @Update(pipeline = {
            "{ '$set': { 'current_step': ?1, 'updated_at': ?2 } }"
    })
    void updateCurrentStepWhereId(
            UUID id,
            String currentStep,
            Instant updatedAt
    );


    @Query("{ '_id' : ?0 }")
    @Update(pipeline = {
            "{ '$set': { " +
                    "'status': ?1, " +
                    "'video_file_paths': ?2, " +
                    "'subtitle_file_paths': ?3, " +
                    "'updated_at': ?4 " +
                    "} }"
    })
    void enqueueForEncoding(
            UUID id,
            UploadJob.UploadStatus status,
            Map<String, String> videoFilePaths,
            Map<String, String> subtitleFilePaths,
            Instant updatedAt
    );


    Page<UploadJob> findAllByUserIdOrderByCreatedAtDesc(
            UUID userId,
            Pageable pageable
    );
}
