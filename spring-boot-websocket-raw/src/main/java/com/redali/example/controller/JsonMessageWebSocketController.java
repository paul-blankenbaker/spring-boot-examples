package com.redali.example.controller;

import com.redali.example.service.SessionService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * WebSocket controller (no STOMP), raw text messages that are then JSON parsed by hand.
 */
@Slf4j
public class JsonMessageWebSocketController extends TextWebSocketHandler {

    private final SessionService sessionService;

    public JsonMessageWebSocketController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    /**
     * Handle text messages posted by the client.
     *
     * <p>This handler requires that each message sent by the client is a valid JSON object. If a client
     * sends something we don't like, we'll shutdown the connection. The only validation done here is that
     * the text message can be converted to a JSON object, then that object is forwarded to the associated
     * service handler for processing.</p>
     *
     * @param session WebSocket client session that posted the message.
     * @param message Message posted by the client.
     * @throws Exception If there is a problem handling the message.
     */
    @Override
    public void handleTextMessage(@NotNull WebSocketSession session, @NotNull TextMessage message) throws Exception {
        String content = message.getPayload();
        try {
            // Parse JSON message client, process and send out response if required
            JSONObject jsonReceived = new JSONObject(content);
            JSONObject jsonResponse = sessionService.processRequest(session, jsonReceived);
            if (jsonResponse != null) {
                session.sendMessage(new TextMessage(jsonResponse.toString()));
            }
        } catch (JSONException err) {
            log.error("Received garbage from {}, closing connection, garbage: {}", session.getRemoteAddress(), content);
            session.close();
            throw err;
        }
    }

    /**
     * When a client opens a connection, we register the session with the associated service.
     *
     * @param session Client session that was just established on the WebSocket.
     * @throws Exception If there is a problem handling the client (should not be - part of interface).
     */
    @Override
    public void afterConnectionEstablished(@NotNull WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        log.info("Connection established from {}", session.getRemoteAddress());
        // Add to list of active sessions
        sessionService.registerSession(session);
    }

    /**
     * When a client connection is closed, we "unregister" it from the associated session service.
     *
     * @param session Client session that was just closed.
     * @throws Exception If there is a problem closing out the client (should not be - part of interface).
     */
    @Override
    public void afterConnectionClosed(@NotNull WebSocketSession session, @NotNull CloseStatus closeStatus) throws Exception {
        log.info("session {} closed", session.getRemoteAddress());
        // Remove from list of active sessions
        sessionService.unregisterSession(session);
        super.afterConnectionClosed(session, closeStatus);
    }

}