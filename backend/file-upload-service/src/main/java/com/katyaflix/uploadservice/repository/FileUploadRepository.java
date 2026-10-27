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

}
