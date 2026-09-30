package com.katyaflix.uploadservice.config;

import com.katyaflix.uploadservice.websocket.UploadJobWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final UploadJobWebSocketHandler uploadJobWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
                .addHandler(uploadJobWebSocketHandler, "/ws/upload-jobs/**")
                .setAllowedOriginPatterns("*");
    }
}
