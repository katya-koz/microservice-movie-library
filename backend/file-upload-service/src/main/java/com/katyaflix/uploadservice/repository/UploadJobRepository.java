package com.katyaflix.uploadservice.repository;

import com.katyaflix.uploadservice.entity.UploadJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface UploadJobRepository extends MongoRepository<UploadJob, UUID> {




    Page<UploadJob> findAllByUserIdOrderByCreatedAtDesc(
            UUID userId,
            Pageable pageable
    );

    void updateFileUploadStatusWhereId(UUID id, boolean b, Instant now);
}
