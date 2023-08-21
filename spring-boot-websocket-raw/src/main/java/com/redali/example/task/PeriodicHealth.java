package com.redali.example.task;

import com.redali.example.service.SessionService;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Collection;

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
        Collection<WebSocketSession> activeSessions = sessionService.getSessions();
        JSONObject health = new JSONObject();
        health.put("id", 200);
        health.put("sessionCount", activeSessions.size());
        health.put("status", "UP");
        TextMessage textMessage = new TextMessage(health.toString());
        log.info("Posting health message: {}", textMessage.getPayload());
        activeSessions.forEach(session -> sendMessage(session, textMessage));
    }

    /**
     * Sends a text message to a client.
     *
     * <p>WARNING: This uses the sendMessage() of the client directly for sending out the message.
     * If one client could cause this to block, that could cause issues for other clients! If the send fails, the
     * client connection will be removed and closed out.</p>
     *
     * @param session Session to send message to.
     * @param textMessage Message to send.
     */
    private void sendMessage(WebSocketSession session, TextMessage textMessage) {
        try {
            session.sendMessage(textMessage);
        } catch (Exception error) {
            log.error("Failed to send message to client {}", session.getRemoteAddress());
            sessionService.unregisterSession(session);
            try {
                session.close();
            } catch (IOException e) {
                log.error("Failed to close WebSocket connection to client {}", session.getRemoteAddress());
            }
        }
    }

}
