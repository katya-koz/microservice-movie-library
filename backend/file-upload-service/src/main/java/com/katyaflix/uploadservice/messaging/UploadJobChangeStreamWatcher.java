package com.katyaflix.uploadservice.messaging;

import com.katyaflix.uploadservice.entity.UploadJob;
import com.katyaflix.uploadservice.websocket.UploadJobWebSocketHandler;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.FullDocument;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Watches the upload_jobs collection with a native Mongo change stream and
 * re-broadcasts every insert/update as a websocket push, so the "current
 * step" the DB already tracks (job.status / job.currentStep) shows up live
 * in the frontend without the frontend having to poll.
 *
 * This is a change stream (not, say, explicit broadcast calls sprinkled
 * through UploadService/FileFinalizationService/etc.) specifically so that
 * it picks up status changes made anywhere - including the catalog-side
 * Kafka listeners this file set doesn't include - without needing every one
 * of those call sites individually wired up to a broadcaster.
 *
 * Requires MongoDB to be running as a replica set (a single-node replica
 * set is fine for local/dev). Given this app already uses @Transactional
 * against Mongo elsewhere, that's presumably already the case - Mongo
 * multi-document transactions have the same requirement.
 */
@Component
public class UploadJobChangeStreamWatcher {

    private static final Logger log = LoggerFactory.getLogger(UploadJobChangeStreamWatcher.class);

    private final MongoTemplate mongoTemplate;
    private final UploadJobWebSocketHandler webSocketHandler;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "upload-job-change-stream");
        thread.setDaemon(true);
        return thread;
    });

    private volatile boolean running = true;

    public UploadJobChangeStreamWatcher(MongoTemplate mongoTemplate, UploadJobWebSocketHandler webSocketHandler) {
        this.mongoTemplate = mongoTemplate;
        this.webSocketHandler = webSocketHandler;
    }

    @PostConstruct
    public void start() {
        executor.submit(this::watchLoop);
    }

    @PreDestroy
    public void stop() {
        running = false;
        executor.shutdownNow();
    }

    private void watchLoop() {
        while (running) {
            try {
                watchOnce();
            } catch (Exception e) {
                if (running) {
                    log.warn("Upload job change stream interrupted, retrying in 3s", e);
                    sleep(3000);
                }
            }
        }
    }

    private void watchOnce() {
        MongoCollection<Document> collection = mongoTemplate.getCollection("upload_jobs");

        try (MongoCursor<ChangeStreamDocument<Document>> cursor = collection.watch()
                .fullDocument(FullDocument.UPDATE_LOOKUP)
                .iterator()) {

            log.info("Upload job change stream started");

            while (running && cursor.hasNext()) {
                ChangeStreamDocument<Document> change = cursor.next();
                Document fullDocument = change.getFullDocument();

                if (fullDocument == null) {
                    continue; // e.g. a delete event - nothing to broadcast
                }

                // Read through the app's own MongoConverter rather than
                // hand-parsing BSON, so this stays correct regardless of how
                // UUIDs/enums are configured to be represented.
                UploadJob job = mongoTemplate.getConverter().read(UploadJob.class, fullDocument);
                webSocketHandler.broadcastJobUpdate(job);
            }
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}