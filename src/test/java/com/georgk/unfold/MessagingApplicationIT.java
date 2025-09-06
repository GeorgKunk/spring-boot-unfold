package com.georgk.unfold;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.*;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MessagingApplicationIT {

    @TestConfiguration(proxyBeanMethods = false)
    static class ContainersConfig {
        @Bean
        @ServiceConnection
        PostgreSQLContainer<?> postgres() {
            return new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("app")
                    .withUsername("postgres")
                    .withPassword("postgres");
        }
    }

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    HttpHeaders halJsonHeaders;

    @BeforeAll
    void setup() {
        halJsonHeaders = new HttpHeaders();
        halJsonHeaders.setContentType(MediaType.APPLICATION_JSON);
        halJsonHeaders.setAccept(List.of(MediaType.valueOf(MediaTypes.HAL_JSON_VALUE)));
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    @Test
    void endToEndMessagingFlow() {
        // Create users
        UUID alice = createUser("alice");
        UUID bob = createUser("bob");
        UUID carol = createUser("carol");

        // Create/get direct thread
        UUID directId = putDirect(alice, bob);
        assertThat(directId).isNotNull();

        // Post messages in direct thread
        UUID m1 = postMessage(directId, alice, "Hi Bob!");
        UUID m2 = postMessage(directId, bob, "Hi Alice!");

        // List messages and verify
        Map<String, Object> directMessages = getMessages(directId, 0, 10);
        assertThat(directMessages).containsKey("_embedded");
        Map<?, ?> embedded = (Map<?, ?>) directMessages.get("_embedded");
        List<?> items = (List<?>) embedded.get("messageModelList");
        assertThat(items).hasSize(2);

        // Create group thread with initial message
        UUID groupId = createGroup(List.of(alice, bob, carol), "Team Chat", alice, "Welcome team!");
        assertThat(groupId).isNotNull();

        // Post more messages
        postMessage(groupId, bob, "Hello all!");
        postMessage(groupId, carol, "Hi!");

        // Verify each user sees group thread in their thread list (paged)
        assertThat(listUserThreads(bob)).anyMatch(id -> id.equals(groupId));
        assertThat(listUserThreads(alice)).anyMatch(id -> id.equals(groupId));
        assertThat(listUserThreads(carol)).anyMatch(id -> id.equals(groupId));

        // Verify HATEOAS links on thread
        ResponseEntity<Map> threadResp = rest.exchange(baseUrl() + "/threads/" + groupId, HttpMethod.GET, new HttpEntity<>(halJsonHeaders), Map.class);
        assertThat(threadResp.getStatusCode().is2xxSuccessful()).isTrue();
        @SuppressWarnings("unchecked")
        Map<String, Object> links = (Map<String, Object>) threadResp.getBody().get("_links");
        assertThat(links).containsKeys("self", "messages", "send-message");

        // Actuator health
        ResponseEntity<Map> health = rest.getForEntity(baseUrl() + "/actuator/health", Map.class);
        assertThat(health.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(((Map<?, ?>) health.getBody()).get("status")).isIn("UP", "DOWN", "UNKNOWN"); // should be UP
    }

    private UUID createUser(String username) {
        Map<String, String> req = Map.of("username", username);
        ResponseEntity<Map> resp = rest.exchange(URI.create(baseUrl() + "/users"), HttpMethod.POST, new HttpEntity<>(req, halJsonHeaders), Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return UUID.fromString(resp.getBody().get("id").toString());
    }

    private UUID putDirect(UUID u1, UUID u2) {
        Map<String, Object> req = Map.of("user1Id", u1.toString(), "user2Id", u2.toString());
        ResponseEntity<Map> resp = rest.exchange(baseUrl() + "/threads/direct", HttpMethod.PUT, new HttpEntity<>(req, halJsonHeaders), Map.class);
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        return UUID.fromString(resp.getBody().get("id").toString());
    }

    private UUID createGroup(List<UUID> participantIds, String name, UUID senderId, String initialMessage) {
        Map<String, Object> req = Map.of(
                "participantIds", participantIds.stream().map(UUID::toString).toList(),
                "name", name,
                "senderId", senderId.toString(),
                "initialMessage", initialMessage
        );
        ResponseEntity<Map> resp = rest.exchange(baseUrl() + "/threads/group", HttpMethod.POST, new HttpEntity<>(req, halJsonHeaders), Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return UUID.fromString(resp.getBody().get("id").toString());
    }

    private UUID postMessage(UUID threadId, UUID senderId, String content) {
        Map<String, Object> req = Map.of("senderId", senderId.toString(), "content", content);
        ResponseEntity<Map> resp = rest.exchange(baseUrl() + "/threads/" + threadId + "/messages", HttpMethod.POST, new HttpEntity<>(req, halJsonHeaders), Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return UUID.fromString(resp.getBody().get("id").toString());
    }

    private Map<String, Object> getMessages(UUID threadId, int page, int size) {
        ResponseEntity<Map> resp = rest.exchange(baseUrl() + "/threads/" + threadId + "/messages?page=" + page + "&size=" + size, HttpMethod.GET, new HttpEntity<>(halJsonHeaders), Map.class);
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        return resp.getBody();
    }

    private List<UUID> listUserThreads(UUID userId) {
        ResponseEntity<Map> resp = rest.exchange(baseUrl() + "/users/" + userId + "/threads", HttpMethod.GET, new HttpEntity<>(halJsonHeaders), Map.class);
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        Map<?, ?> embedded = (Map<?, ?>) resp.getBody().get("_embedded");
        List<Map<String, Object>> items = (List<Map<String, Object>>) embedded.get("threadModelList");
        return items.stream().map(item -> UUID.fromString(item.get("id").toString())).toList();
    }
}
