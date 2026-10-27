package com.katyaflix.uploadservice.service;

import com.katyaflix.uploadservice.dto.enums.PipelineStatus;
import org.springframework.data.mongodb.core.query.Update;

import java.time.Instant;

/**
 * Builds Mongo updates for the per-stage PipelineState subdocuments on FileUpload.
 * Uses dotted paths ($set file_encode_state.status ...) so the errors history is never
 * overwritten, and returns the Update so it can be combined with other field changes
 * in a single atomic write.
 */
public final class FileUploadStages {

    private FileUploadStages() {}

    public enum Stage {
        ASSET("asset_state"),
        CATALOG_VALIDATION("catalog_validation_state"),
        FILE_ENCODE("file_encode_state"),
        CATALOG_ENRICHMENT("catalog_enrichment_state");

        final String field;

        Stage(String field) {
            this.field = field;
        }
    }

    /** Set a stage's status + message. A FAILED status also appends the message to the stage's errors. */
    public static Update stage(Update u, Stage stage, PipelineStatus status, String message) {
        Instant now = Instant.now();
        u.set(stage.field + ".status", status)
                .set(stage.field + ".message", message)
                .set(stage.field + ".updated_at", now)
                .set("updated_at", now);
        if (status == PipelineStatus.FAILED && message != null) {
            u.push(stage.field + ".errors", message);
        }
        return u;
    }

    /** Append to a stage's errors without changing its status (e.g. a failed attempt that will be retried). */
    public static Update error(Update u, Stage stage, String message) {
        return u.push(stage.field + ".errors", message);
    }
}