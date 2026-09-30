package com.katyaflix.uploadservice.repository;

import com.katyaflix.uploadservice.dto.EncodedMediaFile;
import com.katyaflix.uploadservice.dto.FileUploadMetadata;
import com.katyaflix.uploadservice.entity.UploadJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;


public interface UploadJobRepository extends MongoRepository<UploadJob, UUID> {

    @Query("{ '_id' : ?0 }")
    @Update(pipeline = {
            "{ '$set': { 'status': ?1, 'updated_at': '$$NOW' } }"
    })
    void updateStatusWhereId(UUID id, UploadJob.UploadStatus status);

    @Query("{ '_id' : ?0 }")
    @Update(pipeline = {
            "{ '$set': { 'media_file_enrichment_status': ?1, 'updated_at': '$$NOW' } }"
    })
    void updateMediaEnrichmentStatusWhereId(UUID id, boolean mediaEnrichmentStatus);

    @Query("{ '_id' : ?0 }")
    @Update(pipeline = {
            "{ '$set': { 'file_path_update_status': ?1, 'updated_at': '$$NOW' } }"
    })

    void updateFilePathUpdateStatusWhereId(UUID id, boolean filePathUpdateStatus);

    @Query("{ '_id' : ?0 }")
    @Update(pipeline = {
            "{ '$set': { 'catalog_validation_status': ?1, 'updated_at': '$$NOW' } }"
    })


    void updateFileUploadStatusWhereId(UUID id, boolean fileUploadStatus);

    @Query("{ '_id' : ?0 }")
    @Update(pipeline = {
            "{ '$set': { 'file_upload_status': ?1, 'updated_at': '$$NOW' } }"
    })

    void updateCatalogValidationStatusWhereId(UUID id, boolean catalogValidationStatus);


    @Query("{ '_id' : ?0 }")
    @Update(pipeline = {
            "{ '$set': { 'catalog_payload': ?1, 'updated_at': '$$NOW' } }"
    })
    void updatePayloadWhereId(UUID id, FileUploadMetadata.UploadMetadataDto payload);

    /**
     * add to errors list
     */
    @Query("{ '_id' : ?0 }")
    @Update("{ '$push': { 'error_messages': ?1 }, '$set': { 'updated_at': '$$NOW' } }")
    void appendErrorMessage(UUID id, String message);

    @Query("{ '_id' : ?0 }")
    @Update(pipeline = {
            "{ '$set': { 'completed_at': '$$NOW', 'updated_at': '$$NOW' } }"
    })
    void updateCompletedAtWhereId(UUID jobId);



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

    Page<UploadJob>  findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("{ '_id' : ?0 }")
    @Update(pipeline = {
            "{ '$set': { 'current_step': ?1, 'updated_at': '$$NOW' } }"
    })
    void updateCurrentStepWhereId(UUID id, String currentStep);


    @Query("{ '_id' : ?0 }")
    @Update(pipeline = {
            "{ '$set': { " +
                    "'status': ?1, " +
                    "'video_file_paths': ?2, " +
                    "'subtitle_file_paths': ?3, " +
                    "'updated_at': '$$NOW' " +
                    "} }"
    })
    void enqueueForEncoding(
            UUID id,
            UploadJob.UploadStatus status,
            Map<String, String> videoFilePaths,
            Map<String, String> subtitleFilePaths
    );

    Page<UploadJob> findAllByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}

