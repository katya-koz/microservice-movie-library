package com.katyaflix.uploadservice.entity;

import com.katyaflix.uploadservice.dto.metadata.MediaMetadata;
import com.katyaflix.uploadservice.dto.enums.PipelineStatus;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.time.Instant;
import java.util.UUID;

@Document(collection = "upload_jobs")
@Getter
@Setter
public class UploadJob {

    @Id
    private UUID id = UUID.randomUUID();

    @Field("media_type")
    private String mediaType;

    @Field("tmdb_id")
    private long tmdbId;

    @Field("user_id")
    private UUID userId;

    @Field("title")
    private String title;

    @Field("validation_status")
    private PipelineStatus validationStatus = PipelineStatus.IN_PROGRESS;

    @Field("media_metadata")
    private MediaMetadata mediaMetadata;

    @Field("created_at")
    private Instant createdAt = Instant.now();

    @Field("updated_at")
    private Instant updatedAt = Instant.now();

}