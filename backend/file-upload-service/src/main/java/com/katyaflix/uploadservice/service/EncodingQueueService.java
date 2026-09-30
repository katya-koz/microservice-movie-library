package com.katyaflix.uploadservice.service;

import com.katyaflix.uploadservice.entity.UploadJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Enforces a hard cap on how many uploads can be running ffmpeg at once.
 *
 * Jobs are queued by UploadService (status -> QUEUED_FOR_ENCODING, with the
 * saved-but-unencoded file paths stashed on the document). This service
 * polls the upload_jobs collection on a fixed interval and, whenever there
 * are free slots (fewer than encoding.max-concurrent-jobs documents
 * currently in ENCODING status), atomically claims the oldest queued job via
 * Mongo's findAndModify and hands it to a bounded worker pool.
 *
 * findAndModify is atomic per-document, so this is safe even if this
 * service is ever scaled out to multiple upload-service instances: two
 * instances polling concurrently can never both claim the same job, because
 * only one of their findAndModify calls will actually match+update it - the
 * loser's query simply won't find that document anymore.
 */
@Service
public class EncodingQueueService {

    private static final Logger log = LoggerFactory.getLogger(EncodingQueueService.class);

    private final MongoTemplate mongoTemplate;
    private final UploadService uploadService;
    private final int maxConcurrentEncodingJobs;
    private final ExecutorService encodingExecutor;

    public EncodingQueueService(
            MongoTemplate mongoTemplate,
            UploadService uploadService,
            @Value("${encoding.max-concurrent-jobs:2}") int maxConcurrentEncodingJobs
    ) {
        this.mongoTemplate = mongoTemplate;
        this.uploadService = uploadService;
        this.maxConcurrentEncodingJobs = Math.max(1, maxConcurrentEncodingJobs);

        AtomicInteger threadCount = new AtomicInteger(1);
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable, "encoding-worker-" + threadCount.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };

        // The bounded pool is a second, belt-and-suspenders enforcement of
        // the same limit as the queue's own slot check below.
        this.encodingExecutor = Executors.newFixedThreadPool(this.maxConcurrentEncodingJobs, threadFactory);

        log.info("Encoding queue starting with max {} concurrent job(s)", this.maxConcurrentEncodingJobs);
    }

    @Scheduled(fixedDelay = 2000)
    public void pollQueue() {
        try {
            int available = availableSlots();

            for (int i = 0; i < available; i++) {
                UploadJob claimed = claimNextQueuedJob();

                if (claimed == null) {
                    break; // nothing left in the queue
                }

                log.info("Claimed job {} for encoding ({} available slot(s))", claimed.getId(), available - i);
                encodingExecutor.submit(() -> uploadService.runEncodingForJob(claimed));
            }
        } catch (Exception e) {
            log.error("Encoding queue poll failed", e);
        }
    }

    private int availableSlots() {
        long inFlight = mongoTemplate.count(
                Query.query(Criteria.where("status").is(UploadJob.UploadStatus.ENCODING)),
                UploadJob.class
        );

        return (int) Math.max(0, maxConcurrentEncodingJobs - inFlight);
    }

    private UploadJob claimNextQueuedJob() {
        Query query = Query.query(Criteria.where("status").is(UploadJob.UploadStatus.QUEUED_FOR_ENCODING))
                .with(Sort.by(Sort.Direction.ASC, "queued_at"))
                .limit(1);

        Update update = new Update()
                .set("status", UploadJob.UploadStatus.ENCODING)
                .set("encoding_started_at", Instant.now())
                .set("current_step", "Starting encode")
                .set("updated_at", Instant.now());

        return mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true),
                UploadJob.class
        );
    }
}