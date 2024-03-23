package com.redali.example.controller;

import com.redali.example.service.SessionService;
import com.redali.example.service.SessionServiceImpl;
import com.redali.example.task.PeriodicHealth;
import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.CloseReason;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Slf4j
class JsonMessageWebSocketControllerTest {
    private static final String GARBAGE_JSON = "[ { ] ] }";
    private static final String TEST_ENDPOINT = "/websocket/json/messages";
    private static final String CONTENT_KEY = "content";

    @LocalServerPort // The random port number used by our app during the integration test run
    private int serverPort;

    @Autowired // Spring should fill this in for us from our configuration
    private SessionService sessionService;

    @Test
    void testGoodWebSocketInteraction() throws Exception {
        var endPoint = new TestEndpoint();
        var session = createSession(endPoint);
        assertThat(session).isNotNull();
        assertThat(session.isOpen()).isTrue();
        var messageHandler = new TestMessageHandler();
        session.addMessageHandler(messageHandler);

        // Send session info request
        session.getBasicRemote().sendText("{ \"request\": 0 }");
        var got = new JSONObject(messageHandler.waitForMessage());
        assertThat(got).isNotNull();
        assertThat(got.getInt("id")).isZero();

        var server = got.getJSONObject(CONTENT_KEY).getJSONObject("server");
        assertThat(server.getInt("port")).isEqualTo(serverPort);
        assertThat(server.getString("address")).isNotEmpty();
        assertThat(server.getString("host")).isNotEmpty();
        server.getBoolean("resolved"); // hard to know if host name will be resolved, just check existence

        var clt = got.getJSONObject(CONTENT_KEY).getJSONObject("client");
        assertThat(clt).isNotNull();
        assertThat(clt.getInt("port")).isPositive();

        // Send roll dice request
        session.getBasicRemote().sendText("{ \"request\": 1, \"dice\": 3, \"sides\": 12 }");
        got = new JSONObject(messageHandler.waitForMessage());
        assertThat(got).isNotNull();
        assertThat(got.getInt("id")).isEqualTo(1);
        var dice = got.getJSONObject(CONTENT_KEY);
        assertThat(dice).isNotNull();
        assertThat(dice.getInt("dice")).isEqualTo(3);
        assertThat(dice.getInt("sides")).isEqualTo(12);
        var rolls = dice.getJSONArray("rolls");
        var numDice = rolls.length();
        assertThat(numDice).isEqualTo(3);
        for (int i = 0; i < numDice; i++) {
            var die = rolls.getInt(i);
            assertThat(die).isLessThanOrEqualTo(12);
            assertThat(die).isPositive();
        }

        var periodic = new PeriodicHealth(sessionService);
        periodic.periodicHealth();
        got = new JSONObject(messageHandler.waitForMessage());
        assertThat(got.getInt("id")).isEqualTo(200);
        var health = got.getJSONObject(CONTENT_KEY);
        assertThat(health.getInt("sessionCount")).isEqualTo(1);
        assertThat(health.getString("status")).isEqualTo("UP");

        // Verify session is still open
        assertThat(session.isOpen()).isTrue();
    }

    @Test
    void testBadMessageId() throws Exception {
        var endPoint = new TestEndpoint();
        var session = createSession(endPoint);
        assertThat(session).isNotNull();
        assertThat(session.isOpen()).isTrue();
        var clientSessions = sessionService.getSessions();
        assertThat(clientSessions).hasSize(1);
        var clientSession = sessionService.getSessions().stream().findFirst();

        // Send a good JSON message, but with bad access token
        session.getBasicRemote().sendText("{ \"request\": -1 }");
        Boolean closed = endPoint.waitForClose();
        assertThat(closed).isTrue();
        // Attempting to send to closed session should log an error, but not throw an exception
        clientSession.ifPresent(webSocketSession -> sessionService.sendToSession(webSocketSession, -1, null));
    }

    @Test
    void testMissingMessageId() throws Exception {
        var endPoint = new TestEndpoint();
        var session = createSession(endPoint);
        assertThat(session).isNotNull();
        assertThat(session.isOpen()).isTrue();

        // Send a good JSON message, but with bad access token
        session.getBasicRemote().sendText("{ }");
        var closed = endPoint.waitForClose();
        assertThat(closed).isTrue();
    }

    @Test
    void testBadJSONWebSocketInteraction() throws Exception {
        var endPoint = new TestEndpoint();
        var session = createSession(endPoint);
        assertThat(session).isNotNull();
        assertThat(session.isOpen()).isTrue();

        // Send bad json message
        session.getBasicRemote().sendText(GARBAGE_JSON);
        var closed = endPoint.waitForClose();
        assertThat(closed).isTrue();
    }

    // Test helper method to get a client connection to the WebSocket server and return the session
    private Session createSession(Endpoint endPoint) throws DeploymentException, IOException {
        var client = ContainerProvider.getWebSocketContainer();
        var uri = URI.create("ws://localhost:" + serverPort + TEST_ENDPOINT);

        var configurator = new ClientEndpointConfig.Configurator();
        var clientConfigEndPoint = ClientEndpointConfig.Builder.create().configurator(configurator).build();
        return client.connectToServer(endPoint, clientConfigEndPoint, uri);
    }

    private static class TestEndpoint extends Endpoint {
        private final BlockingQueue<Boolean> closeStates = new LinkedBlockingQueue<>();

        @Override
        public void onOpen(Session session, EndpointConfig endpointConfig) {
        }

        @Override
        public void onClose(Session session, CloseReason reason) {
            closeStates.add(Boolean.TRUE);
        }

        Boolean waitForClose() throws InterruptedException {
            return closeStates.poll(1, TimeUnit.SECONDS);
        }
    }

    private static class TestMessageHandler implements MessageHandler.Whole<String> {
        private final BlockingQueue<String> receivedMessages = new LinkedBlockingQueue<>();

        @Override
        public void onMessage(String message) {
            log.info("Received message {}", message);
            receivedMessages.add(message);
        }

        String waitForMessage() throws InterruptedException {
            return receivedMessages.poll(1, TimeUnit.SECONDS);
        }
    }

    /**
     * Configuration for integration tests.
     */
    @TestConfiguration()
    public static class Config {

        @Bean
        @Primary
        public SessionService sessionServiceTest() {
            return new SessionServiceImpl();
        }
    }

}