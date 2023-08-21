package com.redali.example.config;

import com.redali.example.controller.JsonMessageWebSocketController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Tells spring that we want to register a WebSocket controller at a specific endpoint.
 */
@Configuration
@EnableWebSocket
@Slf4j
public class JsonMessageWebSocketConfig implements WebSocketConfigurer {

    private final JsonMessageWebSocketController controller;
    private final String endPoint;

    // Leave the end point configurable in application.yaml (spring will inject it for us)
    public JsonMessageWebSocketConfig(JsonMessageWebSocketController sessionController,
                                      @Value(value="${app.endpoint.json.messages}") String endPoint) {
        this.controller = sessionController;
        this.endPoint = endPoint;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // NOTE: You may want to change or remove the allowed origins, when scripting clients with Python
        // I found it easier to set it to "*", but this is probably not a good practice if your intention
        // is for WebSocket client connections
        registry.addHandler(controller, endPoint)
                .setAllowedOriginPatterns("*");
        log.info("Registered JSON message handler WebSocket endpoint at: {}", endPoint);
    }
}
