package com.katyaflix.uploadservice.websocket;

import com.katyaflix.uploadservice.dto.FileStatusDto;
import com.katyaflix.uploadservice.entity.FileUpload;
import com.katyaflix.uploadservice.entity.UploadJob;
import com.katyaflix.uploadservice.repository.FileUploadRepository;
import com.katyaflix.uploadservice.service.UploadService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * /ws/upload-pipeline/{jobId}?userId=...

 */
@Component
@RequiredArgsConstructor
public class FileStatusWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(FileStatusWebSocketHandler.class);
    private static final String JOB_ATTR = "jobId";

    private final UploadService uploadService;
    private final FileUploadRepository fileUploadRepository;
    private final tools.jackson.databind.ObjectMapper mapper;

    // jobId -> (raw session id -> thread-safe wrapper)
    private final Map<UUID, Map<String, WebSocketSession>> sessionsByJob = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession raw) throws Exception {
        UUID jobId;
        UUID userId;
        try {
            var uri = UriComponentsBuilder.fromUri(raw.getUri()).build();
            var segments = uri.getPathSegments();
            jobId = UUID.fromString(segments.get(segments.size() - 1));
            userId = UUID.fromString(uri.getQueryParams().getFirst("userId")); // TODO: auth principal
        } catch (Exception e) {
            raw.close(CloseStatus.BAD_DATA);
            return;
        }

        // missing job or someone else's job: same refusal, so job ids can't be probed
        Optional<UploadJob> job = uploadService.findJob(jobId);
        if (job.isEmpty() || !userId.equals(job.get().getUserId())) {
            raw.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        raw.getAttributes().put(JOB_ATTR, jobId);
        WebSocketSession session = new ConcurrentWebSocketSessionDecorator(raw, 5_000, 512 * 1024);

        // register first, snapshot second: a change landing in between is delivered twice at
        // worst, never missed (the client applies statuses idempotently)
        sessionsByJob.computeIfAbsent(jobId, k -> new ConcurrentHashMap<>()).put(raw.getId(), session);

        List<FileStatusDto> files = fileUploadRepository.findByUploadJobId(jobId).stream()
                .map(FileStatusDto::from)
                .toList();
        send(session, Map.of("type", "snapshot", "files", files));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession raw, CloseStatus status) {
        Object jobId = raw.getAttributes().get(JOB_ATTR);
        if (jobId instanceof UUID id) {
            Map<String, WebSocketSession> sessions = sessionsByJob.get(id);
            if (sessions != null) {
                sessions.remove(raw.getId());
                if (sessions.isEmpty()) sessionsByJob.remove(id, sessions);
            }
        }
    }

    /** Called for every file_uploads change. */
    public void broadcast(FileUpload file) {
        Map<String, WebSocketSession> sessions = sessionsByJob.get(file.getUploadJobId());
        if (sessions == null || sessions.isEmpty()) return;

        Map<String, Object> payload = Map.of("type", "file", "file", FileStatusDto.from(file));
        sessions.values().forEach(s -> send(s, payload));
    }

    private void send(WebSocketSession session, Object payload) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(mapper.writeValueAsString(payload)));
            }
        } catch (Exception e) {
            log.debug("Could not push to websocket session {}: {}", session.getId(), e.getMessage());
        }
    }
}