//package com.katyaflix.userservice.config;
//
//import com.katyaflix.userservice.websocket.WatchtimeWebSocketHandler;
//import lombok.RequiredArgsConstructor;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.socket.config.annotation.EnableWebSocket;
//import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
//import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
//
//@Configuration
//@EnableWebSocket
//@RequiredArgsConstructor
//public class WebSocketConfig implements WebSocketConfigurer {
//
//    private final WatchtimeWebSocketHandler watchtimeWebSocketHandler;
//
//    @Override
//    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
//        registry.addHandler(watchtimeWebSocketHandler, "/ws/watchtime")
//                // Tighten this to your frontend's real origin(s) in production.
//                .setAllowedOriginPatterns("*");
//    }
//}
