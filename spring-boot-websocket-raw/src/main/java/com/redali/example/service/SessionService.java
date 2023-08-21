package com.redali.example.service;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collection;

/**
 * Defines what we need our JSON message processing service to do.
 */
public interface SessionService {
    /**
     * Looks at the "request" and optional "content" fields of the payload JSON message and responds accordingly.
     *
     * @param session      Session that JSON message was received from.
     * @param jsonReceived JSON request message in form of { "id": REQUEST_TYPE_ID } or { "id": REQUEST_TYPE_ID,
     *                     "content": { } } where "content" is dependent on the message type being requested.
     * @return JSONObject to send back to session or null if nothing to send back.
     * @throws JSONException If there was a problem with the request.
     */
    JSONObject processRequest(@NotNull WebSocketSession session, @NotNull JSONObject jsonReceived) throws JSONException;

    /**
     * Used to register a new WebSocket session with the service.
     *
     * @param session WebSocket session to register (must not be null).
     */
    void registerSession(@NotNull WebSocketSession session);

    /**
     * Used to unregister an existing WebSocket session with the service.
     *
     * @param session WebSocket session to unregister (must not be null).
     */
    void unregisterSession(@NotNull WebSocketSession session);

    /**
     * Get a safe copy of all the WebSocket sessions currently registered with the service.
     *
     * @return Collection of 0 or more WebSocket sessions.
     */
    Collection<WebSocketSession> getSessions();
}
