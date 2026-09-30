package com.katyaflix.uploadservice.websocket;

import com.katyaflix.uploadservice.dto.UploadJobStatusDto;
import com.katyaflix.uploadservice.entity.UploadJob;
import com.katyaflix.uploadservice.repository.UploadJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * One websocket connection per job id: connect to /ws/upload-jobs/{jobId} to
 * get the job's current state immediately, then a fresh JSON payload every
 * time it changes (see UploadJobChangeStreamWatcher, which is what actually
 * calls broadcastJobUpdate).
 */
@Component
public class UploadJobWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(UploadJobWebSocketHandler.class);

    private final UploadJobRepository jobRepository;
    private final ObjectMapper objectMapper;

    private final Map<UUID, Set<WebSocketSession>> sessionsByJobId = new ConcurrentHashMap<>();

    public UploadJobWebSocketHandler(UploadJobRepository jobRepository, ObjectMapper objectMapper) {
        this.jobRepository = jobRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        UUID jobId = extractJobId(session);

        if (jobId == null) {
            closeQuietly(session, CloseStatus.BAD_DATA);
            return;
        }

        sessionsByJobId.computeIfAbsent(jobId, id -> new CopyOnWriteArraySet<>()).add(session);

        // send the current snapshot immediately, don't make the client wait
        // for the next change stream event
        jobRepository.findById(jobId).ifPresentOrElse(
                job -> sendQuietly(session, job),
                () -> closeQuietly(session, CloseStatus.NOT_ACCEPTABLE.withReason("Unknown job id"))
        );
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionsByJobId.values().forEach(sessions -> sessions.remove(session));
    }

    public void broadcastJobUpdate(UploadJob job) {
        Set<WebSocketSession> sessions = sessionsByJobId.get(job.getId());

        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        for (WebSocketSession session : sessions) {
            sendQuietly(session, job);
        }
    }

    private void sendQuietly(WebSocketSession session, UploadJob job) {
        try {
            if (!session.isOpen()) {
                return;
            }
            String payload = objectMapper.writeValueAsString(UploadJobStatusDto.from(job));
            session.sendMessage(new TextMessage(payload));
        } catch (Exception e) {
            log.warn("Failed to push job update to websocket session {}", session.getId(), e);
        }
    }

    private void closeQuietly(WebSocketSession session, CloseStatus status) {
        try {
            session.close(status);
        } catch (Exception ignored) {
        }
    }

    private UUID extractJobId(WebSocketSession session) {
        try {
            String path = session.getUri() != null ? session.getUri().getPath() : null;
            if (path == null) return null;

            String[] segments = path.split("/");
            String last = segments[segments.length - 1];

            return UUID.fromString(last);
        } catch (Exception e) {
            log.warn("Could not parse job id from websocket path", e);
            return null;
        }
    }
}