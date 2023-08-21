package com.redali.example.service;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Our implementation of a JSON based messaging service.
 *
 * <p>Clients will send messages in one of two forms. A "request" ID/type is always required. For example a
 * simple "info" request defined as type 0 would be something like:</p>
 *
 * <pre><code>{ "request": 0 }</code></pre>
 *
 * <p>If a request type/id needs additional information, it will indicate what other attributes need to be
 * provided in the request.</p>
 *
 * <p>A message sent back to the client will have a "response" type/ID always present and will likely have
 * additional information provided based on the response type.</p>
 *
 * <pre><code>{ "response": 0, "epochMillis": 381888288332, ... }</code></pre>
 */
@Service
@Slf4j
public class SessionServiceImpl implements SessionService {
    // Used to keep track of active sessions, useful if you want to be able to push
    // server side generated messages out or have messages from one session trigger
    // messages to other sessions.
    private final Map<String, WebSocketSession> sessions = new HashMap<>();
    private final Random diceRoller = new Random();

    @Override
    public void registerSession(@NotNull WebSocketSession session) {
        String id = session.getId();
        synchronized (sessions) {
            sessions.put(id, session);
        }
    }

    @Override
    public void unregisterSession(@NotNull WebSocketSession session) {
        String id = session.getId();
        synchronized (sessions) {
            sessions.remove(id);
        }
    }

    @Override
    public Collection<WebSocketSession> getSessions() {
        synchronized (sessions) {
            return sessions.values();
        }
    }

    @Override
    public JSONObject processRequest(@NotNull WebSocketSession session, @NotNull JSONObject jsonReceived) throws JSONException {
        int messageType = jsonReceived.getInt("request");
        switch (messageType) {
            case 0:
                return wrap(0, createSessionInfo(session));
            case 1:
                return wrap(1, createDiceRoll(jsonReceived));
            default:
                log.warn("Ignored request for type {} message from client {}", messageType, session.getRemoteAddress());
                return null;
        }
    }

    // Example of processing a JSON request that requires additional attributes.
    // Request in the form of { "request": 1, "dice": INT_UP_TO_100, "sides": INT_AT_LEAST_2 }
    private JSONObject createDiceRoll(JSONObject jsonReceived) {
        int dice = Math.min(100, jsonReceived.has("dice") ? jsonReceived.getInt("dice") : 2);
        int sides = Math.max(2, jsonReceived.has("sides") ? jsonReceived.getInt("sides") : 6);
        int[] roll = new int[dice];
        for (int i = 0; i < dice; i++) {
            roll[i] = diceRoller.nextInt(sides) + 1;
        }
        JSONObject results = new JSONObject();
        results.put("dice", dice);
        results.put("sides", sides);
        results.put("rolls", new JSONArray(roll));
        return results;
    }

    private JSONObject createSessionInfo(WebSocketSession session) {
        JSONObject json = new JSONObject();
        // WARNING: JSON has a practical limit of 53 bits on integer numbers
        json.put("epochMillis", System.currentTimeMillis());
        addAddressInfo(json, "server", session.getLocalAddress());
        addAddressInfo(json, "client", session.getRemoteAddress());
        return json;
    }

    private void addAddressInfo(JSONObject message, String key, InetSocketAddress address) {
        if (address == null) {
            return;
        }
        JSONObject json = new JSONObject();
        json.put("address", address.getAddress().getHostAddress());
        json.put("host", address.getHostName());
        json.put("resolved", !address.isUnresolved());
        json.put("port", address.getPort());
        message.put(key, json);
    }

    private JSONObject wrap(int type, JSONObject content) {
        JSONObject json = new JSONObject();
        json.put("response", type);
        json.put("content", content);
        return json;
    }

}
