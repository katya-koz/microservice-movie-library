package com.katyaflix.uploadservice.config;

import com.katyaflix.uploadservice.websocket.FileStatusWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class FileStatusWebSocketConfig implements WebSocketConfigurer {

    private final FileStatusWebSocketHandler handler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/upload-pipeline/*")
                .setAllowedOriginPatterns("*"); // tighten to your frontend origin(s) outside dev
    }
}