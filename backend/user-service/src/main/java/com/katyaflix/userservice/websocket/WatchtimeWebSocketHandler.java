//package com.katyaflix.userservice.websocket;
//
//import com.katyaflix.userservice.dto.MediaType;
//import com.katyaflix.userservice.dto.UpdateWatchtimeRequest;
//import com.katyaflix.userservice.dto.WatchtimeDto;
//import com.katyaflix.userservice.service.WatchtimeService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//import org.springframework.web.socket.CloseStatus;
//import org.springframework.web.socket.TextMessage;
//import org.springframework.web.socket.WebSocketSession;
//import org.springframework.web.socket.handler.TextWebSocketHandler;
//import tools.jackson.databind.ObjectMapper;
//
//import java.util.UUID;
//
///**
// * Handles /ws/watchtime?userId=... — the player connects once per session
// * and periodically sends small JSON pings with its current position:
// *
// *   { "mediaType": "MOVIE" | "EPISODE", "mediaId": "...", "watchtimeSeconds": 123 }
// *
// * Each ping is upserted immediately (see WatchtimeService), which also
// * publishes a WatchtimeUpdatedEvent to Kafka for any other interested
// * consumer (e.g. a "continue watching" recommendation job). An ack is sent
// * back so the client can confirm the save if it wants to.
// */
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class WatchtimeWebSocketHandler extends TextWebSocketHandler {
//
//    private final WatchtimeService watchtimeService;
//    private final ObjectMapper objectMapper;
//
//    private record WatchtimePing(MediaType mediaType, UUID mediaId, long watchtimeSeconds, long durationSeconds) {
//    }
//
//    private record WatchtimeAck(String status, WatchtimeDto watchtime, String error) {
//    }
//
//    @Override
//    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
//        if (extractUserId(session) == null) {
//            session.close(CloseStatus.BAD_DATA.withReason("Missing or invalid ?userId= query param"));
//        }
//    }
//
//    @Override
//    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
//        UUID userId = extractUserId(session);
//        if (userId == null) {
//            return;
//        }
//
//        try {
//            WatchtimePing ping = objectMapper.readValue(message.getPayload(), WatchtimePing.class);
//            UpdateWatchtimeRequest request = new UpdateWatchtimeRequest(ping.mediaType(), ping.mediaId(), ping.watchtimeSeconds(), ping.durationSeconds());
//            WatchtimeDto updated = watchtimeService.upsertWatchtime(userId, request);
//
//            if (session.isOpen()) {
//                session.sendMessage(new TextMessage( objectMapper.writeValueAsString(new WatchtimeAck("ok", updated, null))));
//            }
//        } catch (Exception e) {
//            log.warn("Failed to process watchtime ping for user {}: {}", userId, e.getMessage());
//            if (session.isOpen()) {
//                session.sendMessage(new TextMessage( objectMapper.writeValueAsString(new WatchtimeAck("error", null, e.getMessage()))));
//            }
//        }
//    }
//
//    private UUID extractUserId(WebSocketSession session) {
//        String query = session.getUri() != null ? session.getUri().getQuery() : null;
//        if (query == null) {
//            return null;
//        }
//        for (String param : query.split("&")) {
//            String[] kv = param.split("=", 2);
//            if (kv.length == 2 && kv[0].equals("userId")) {
//                try {
//                    return UUID.fromString(kv[1]);
//                } catch (IllegalArgumentException ex) {
//                    return null;
//                }
//            }
//        }
//        return null;
//    }
//}
