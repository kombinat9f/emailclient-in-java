package de.kombinat9f.emailclient.controller;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

public class EmailControllerTest {

    RestTestClient client;

    @BeforeEach
    public void setup() {
        client = RestTestClient.bindToController(new EmailController()).build();
    }

    @Test
    void postWithBodyReturnsAccepted() {
        Map<String, String> payload = Map.of(
                "to", "user@example.com",
                "subject", "Test",
                "payloadUri", "htttp://cdn.mailpayload.de");

        client.post()
                .uri("/api/email")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .exchange()
                .expectStatus().isAccepted();
    }

    @Test
    void postWithBodyWithoutPayloadUriReturnsAccepted() {
        Map<String, String> payload = Map.of(
                "to", "user@example.com",
                "subject", "Test");

        client.post()
                .uri("/api/email")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .exchange()
                .expectStatus().isAccepted();
    }

    @Test
    void postWithoutBodReturnsBadRequest() {

        client.post()
                .uri("/api/email")
                .exchange()
                .expectStatus().isBadRequest();
    }

}
