package com.redali.example.task;

import com.redali.example.service.SessionService;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Example of a server initiated message to WebSocket client sessions (they don't request this, we just push it onto
 * them).
 */
@Component
@Slf4j
public class PeriodicHealth {

    private final SessionService sessionService;

    public PeriodicHealth(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    /**
     * Example of a server side generated message pushed out to ALL active clients.
     */
    @Scheduled(fixedRate = 9000, initialDelay = 9000)
    public void periodicHealth() {
        var activeSessions = sessionService.getSessions();
        var health = new JSONObject();
        health.put("sessionCount", activeSessions.size());
        health.put("status", "UP");
        activeSessions.forEach(session -> sessionService.sendToSession(session, 200, health));
    }

}
