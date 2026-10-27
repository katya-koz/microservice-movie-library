package com.katyaflix.uploadservice.messaging;

import com.katyaflix.uploadservice.dto.enums.EventStatus;
import com.katyaflix.uploadservice.dto.enums.FileUploadStatus;
import com.katyaflix.uploadservice.dto.enums.PipelineStatus;
import com.katyaflix.uploadservice.dto.mediaenrichment.MediaEnrichmentResult;
import com.katyaflix.uploadservice.dto.mediaenrichment.SubtitleEnrichmentResult;
import com.katyaflix.uploadservice.entity.FileUpload;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static com.katyaflix.uploadservice.service.FileUploadStages.Stage.CATALOG_ENRICHMENT;
import static com.katyaflix.uploadservice.service.FileUploadStages.stage;

/**
 * Receives the catalog's reply to a media/subtitle enrichment event and closes out the FileUpload.
 * The catalog keys these results by fileUploadId.
 */
@Component
@RequiredArgsConstructor
public class EnrichmentResultListener {

    private static final Logger log = LoggerFactory.getLogger(EnrichmentResultListener.class);

    private final MongoTemplate mongo;

    @KafkaListener(
            topics = CatalogTopics.MEDIA_ENRICHMENT_COMPLETED,
            groupId = "upload-service"
    )
    public void onMediaEnrichmentResult(MediaEnrichmentResult result) {
        apply("media", result.fileUploadId(), result.status(), result.message());
    }

    @KafkaListener(
            topics = CatalogTopics.SUBTITLE_ENRICHMENT_COMPLETED,
            groupId = "upload-service"
    )
    public void onSubtitleEnrichmentResult(SubtitleEnrichmentResult result) {
        apply("subtitle", result.fileUploadId(), result.status(), result.message());
    }

    private void apply(String kind, UUID fileUploadId, EventStatus status, String message) {
        boolean ok = status == EventStatus.SUCCESS;

        Update u = new Update();
        if (ok) {
            stage(u, CATALOG_ENRICHMENT, PipelineStatus.COMPLETED,
                    message != null ? message : "Catalog updated");
            u.set("status", FileUploadStatus.COMPLETED)
                    .set("progress_percent", 100)
                    .set("error", null);
        } else {
            String msg = message != null ? message : "Catalog rejected the " + kind + " file";
            stage(u, CATALOG_ENRICHMENT, PipelineStatus.FAILED, msg);
            u.set("status", FileUploadStatus.FAILED)
                    .set("error", "Catalog enrichment failed: " + msg);
        }

        // Only act on files that are actually waiting for the catalog: makes redelivered /
        // duplicate results harmless and stops a late reply overwriting a newer state.
        long matched = mongo.updateFirst(
                Query.query(Criteria.where("_id").is(fileUploadId)
                        .and("status").is(FileUploadStatus.AWAITING_ENRICHMENT)),
                u, FileUpload.class).getMatchedCount();

        if (matched == 0) {
            log.warn("Ignoring {} enrichment result for file {}: not found or not awaiting enrichment",
                    kind, fileUploadId);
        } else if (ok) {
            log.info("File {} enriched in catalog ({})", fileUploadId, kind);
        } else {
            log.warn("Catalog rejected {} enrichment for file {}: {}", kind, fileUploadId, message);
        }
    }
}