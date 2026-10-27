package com.katyaflix.uploadservice.repository;

import com.katyaflix.uploadservice.entity.FileUpload;
import com.katyaflix.uploadservice.entity.UploadJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.data.repository.query.Param;


import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface FileUploadRepository extends MongoRepository<FileUpload, UUID> {

    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'encoding_progress': ?1 } }")
    void updateEncodeProgress(UUID id, int progress);

    List<FileUpload> findByUploadJobId(UUID jobId);
//    // ---------- asset_state_state ----------
//    @Query("{ '_id': ?0 }")
//    @Update("{ '$set': { 'asset_state_state.status': ?1, 'asset_state_state.updated_at': ?2 } }")
//    void updateAssetStatus(UUID id, PipelineStatus status, Instant now);
//
//    @Query("{ '_id': ?0 }")
//    @Update("{ '$push': { 'asset_state_state.errors': ?1 }, '$set': { 'asset_state_state.updated_at': ?2 } }")
//    void addAssetError(UUID id, String error, Instant now);
//
//    @Query("{ '_id': ?0 }")
//    @Update("{ '$set': { 'asset_state_state.status': 'FAILURE', 'asset_state_state.updated_at': ?2 }, '$push': { 'asset_state_state.errors': ?1 } }")
//    void failAsset(UUID id, String error, Instant now);
//
//    // ---------- catalog_validation_state ----------
//    @Query("{ '_id': ?0 }")
//    @Update("{ '$set': { 'catalog_validation_state.status': ?1, 'catalog_validation_state.updated_at': ?2 } }")
//    void updateCatalogValidationStatus(UUID id, PipelineStatus status, Instant now);
//
//    @Query("{ '_id': ?0 }")
//    @Update("{ '$push': { 'catalog_validation_state.errors': ?1 }, '$set': { 'catalog_validation_state.updated_at': ?2 } }")
//    void addCatalogValidationError(UUID id, String error, Instant now);
//
//    @Query("{ '_id': ?0 }")
//    @Update("{ '$set': { 'catalog_validation_state.status': 'FAILURE', 'catalog_validation_state.updated_at': ?2 }, '$push': { 'catalog_validation_state.errors': ?1 } }")
//    void failCatalogValidation(UUID id, String error, Instant now);
//
//    // ---------- file_encode_state ----------
//    @Query("{ '_id': ?0 }")
//    @Update("{ '$set': { 'file_encode_state.status': ?1, 'file_encode_state.updated_at': ?2 } }")
//    void updateFileEncodeStatus(UUID id, PipelineStatus status, Instant now);
//
//    @Query("{ '_id': ?0 }")
//    @Update("{ '$push': { 'file_encode_state.errors': ?1 }, '$set': { 'file_encode_state.updated_at': ?2 } }")
//    void addFileEncodeError(UUID id, String error, Instant now);
//
//    @Query("{ '_id': ?0 }")
//    @Update("{ '$set': { 'file_encode_state.status': 'FAILURE', 'file_encode_state.updated_at': ?2 }, '$push': { 'file_encode_state.errors': ?1 } }")
//    void failFileEncode(UUID id, String error, Instant now);
//
//    // ---------- file_move_state ----------
//    @Query("{ '_id': ?0 }")
//    @Update("{ '$set': { 'file_move_state.status': ?1, 'file_move_state.updated_at': ?2 } }")
//    void updateFileMoveStatus(UUID id, PipelineStatus status, Instant now);
//
//    @Query("{ '_id': ?0 }")
//    @Update("{ '$push': { 'file_move_state.errors': ?1 }, '$set': { 'file_move_state.updated_at': ?2 } }")
//    void addFileMoveError(UUID id, String error, Instant now);
//
//    @Query("{ '_id': ?0 }")
//    @Update("{ '$set': { 'file_move_state.status': 'FAILURE', 'file_move_state.updated_at': ?2 }, '$push': { 'file_move_state.errors': ?1 } }")
//    void failFileMove(UUID id, String error, Instant now);
//
//    // ---------- catalog_enrichment_state ----------
//    @Query("{ '_id': ?0 }")
//    @Update("{ '$set': { 'catalog_enrichment_state.status': ?1, 'catalog_enrichment_state.updated_at': ?2 } }")
//    void updateCatalogEnrichmentStatus(UUID id, PipelineStatus status, Instant now);
//
//    @Query("{ '_id': ?0 }")
//    @Update("{ '$push': { 'catalog_enrichment_state.errors': ?1 }, '$set': { 'catalog_enrichment_state.updated_at': ?2 } }")
//    void addCatalogEnrichmentError(UUID id, String error, Instant now);
//
//    @Query("{ '_id': ?0 }")
//    @Update("{ '$set': { 'catalog_enrichment_state.status': 'FAILURE', 'catalog_enrichment_state.updated_at': ?2 }, '$push': { 'catalog_enrichment_state.errors': ?1 } }")
//    void failCatalogEnrichment(UUID id, String error, Instant now);
//
//    /**
//     * Add to errors list.
//     */
//    @Query("{ '_id' : ?0 }")
//    @Update("{ '$push': { 'error_messages': ?1 }, '$set': { 'updated_at': ?2 } }")
//    void appendErrorMessage(
//            UUID id,
//            String message,
//            Instant updatedAt
//    );
//
//
//    @Query("{ '_id' : ?0 }")
//    @Update(pipeline = {
//            "{ '$set': { 'completed_at': ?1, 'updated_at': ?1 } }"
//    })
//    void updateCompletedAtWhereId(
//            UUID jobId,
//            Instant completedAt
//    );
//
//
//    @Query(value = "{ '_id': ?0 }", fields = "{ 'encoded_media_files': 1 }")
//    Optional<UploadJob> findEncodedMediaFilesById(UUID id);
//
//
//    @Query("{ '_id': ?0 }")
//    @Update("""
//    {
//        "$set": {
//            "encoded_media_files": ?1
//        }
//    }
//    """)
//    long saveEncodedMediaFiles(
//            UUID id,
//            List<EncodedMediaFile> encodedMediaFiles
//    );
//
//
//    Page<UploadJob> findAllByOrderByCreatedAtDesc(Pageable pageable);
//
//
//    @Query("{ '_id' : ?0 }")
//    @Update(pipeline = {
//            "{ '$set': { 'current_step': ?1, 'updated_at': ?2 } }"
//    })
//    void updateCurrentStepWhereId(
//            UUID id,
//            String currentStep,
//            Instant updatedAt
//    );


//    @Query("{ '_id' : ?0 }")
//    @Update(pipeline = {
//            "{ '$set': { " +
//                    "'status': ?1, " +
//                    "'video_file_paths': ?2, " +
//                    "'subtitle_file_paths': ?3, " +
//                    "'updated_at': ?4 " +
//                    "} }"
//    })
//    void enqueueForEncoding(
//            UUID id,
//            UploadJob.UploadStatus status,
//            Map<String, String> videoFilePaths,
//            Map<String, String> subtitleFilePaths,
//            Instant updatedAt
//    );


//    Page<UploadJob> findAllByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);



}
