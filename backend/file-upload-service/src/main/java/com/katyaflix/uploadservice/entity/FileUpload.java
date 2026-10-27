package com.katyaflix.uploadservice.entity;

import com.katyaflix.uploadservice.dto.enums.FileType;
import com.katyaflix.uploadservice.dto.enums.FileUploadStatus;
import com.katyaflix.uploadservice.dto.enums.PipelineStatus;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Document(collection = "file_uploads")
@CompoundIndex(name = "status_queued_idx", def = "{'status': 1, 'queued_at': 1}")
@Getter
@Setter
public class FileUpload {

    @Id
    private UUID id = UUID.randomUUID();

    @Field("upload_job_id")
    private UUID uploadJobId;
    @Field("tmdb_id")
    private long tmdbId;

    @Field("language")
    private String language = "en";

    @Field("file_type")
    private FileType fileType;

    @Field("original_file_name")
    private String originalFileName;

    @Field("file_extension")
    private String fileExtension;

    @Field("status")
    private FileUploadStatus status = FileUploadStatus.UPLOADING;

    // absolute path of the raw upload in temp storage
    @Field("source_path")
    private String sourcePath;

    // path relative to mediaRoot once finalized
    @Field("final_path")
    private String finalPath;

    @Field("progress_percent")
    private int progressPercent;

    @Field("upload_progress")
    private int uploadProgress;

    @Field("encoding_progress")
    private int encodeProgress;

    @Field("attempts")
    private int attempts;

    @Field("error")
    private String error;

    @Field("queued_at")
    private Instant queuedAt;

    @Field("encoding_started_at")
    private Instant encodingStartedAt;

    // updated by the worker every few seconds; stale heartbeat => worker died
    @Field("heartbeat_at")
    private Instant heartbeatAt;

    @Field("asset_state")              private PipelineState assetState;
    @Field("catalog_validation_state") private PipelineState catalogValidationState;
    @Field("file_encode_state")        private PipelineState fileEncodeState;
    @Field("catalog_enrichment_state") private PipelineState catalogEnrichmentState;
}
