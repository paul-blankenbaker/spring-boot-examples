package com.redali.example.config;

import com.redali.example.controller.JsonMessageWebSocketController;
import com.redali.example.service.SessionService;
import com.redali.example.service.SessionServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Give spring a clue about the session service and our WebSocket controller that we'd like injected.
 */
@Configuration
public class SessionServiceConfig {
    @Bean
    public SessionService sessionService() {
        return new SessionServiceImpl();
    }

    @Bean
    public JsonMessageWebSocketController sessionController(SessionService sessionService) {
        return new JsonMessageWebSocketController(sessionService);
    }
}
