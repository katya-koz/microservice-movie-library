package com.katyaflix.uploadservice.entity;

import com.katyaflix.uploadservice.dto.EncodedMediaFile;
import com.katyaflix.uploadservice.dto.FileUploadMetadata;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Document(collection = "upload_jobs")
@Getter
@Setter
public class UploadJob {

    public enum UploadStatus {
        SAVING_ASSETS,
        PENDING,
        UPLOADING,
        ENCODING,
        AWAITING_CATALOG,
        READY_TO_FINALIZE,
        FINALIZING,
        COMPLETED,
        FAILURE
    }

    @Id
    private UUID id = UUID.randomUUID();

    @Field("media_type")
    private String mediaType;

    @Field("status")
    private UploadStatus status = UploadStatus.PENDING;

    @Field("progress_percent")
    private int progressPercent = 0;

    @Field("tmdb_id")
    private long tmdbId;

    @Field("title")
    private String title;

    @Field("catalog_validation_status")
    private Boolean catalogValidationStatus = false;

    @Field("file_upload_status")
    private Boolean fileUploadStatus = false;

    @Field("media_file_enrichment_status")
    private Boolean mediaFileEnrichmentStatus = false;

    @Field("file_path_update_status")
    private Boolean filePathUpdateStatus = false;

    /** left null for now */
    @Field("requested_by")
    private UUID requestedBy;


    @Field("encoded_media_files")
    private List<EncodedMediaFile> encodedMediaFiles;

    @Field("error_messages")
    private List<String> errorMessages = new ArrayList<String>();;

    // native subdocument instead of a jsonb column
    @Field("catalog_payload")
    private FileUploadMetadata.UploadMetadataDto catalogPayload;

    @Field("created_at")
    private Instant createdAt = Instant.now();

    @Field("updated_at")
    private Instant updatedAt = Instant.now();

    @Field("completed_at")
    private Instant completedAt;
}