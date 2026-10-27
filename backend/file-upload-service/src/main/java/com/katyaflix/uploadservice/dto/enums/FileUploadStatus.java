package com.katyaflix.uploadservice.dto.enums;

public enum FileUploadStatus {
    UPLOADING,
    ENQUEUED,
    PENDING,
    ENCODING,
    DOWNLOADING_ASSETS,
    FINALIZING,
    COMPLETED,
    FAILED,
    AWAITING_ENRICHMENT

}
