package de.kombinat9f.emailclient.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.Map;

@ExtendWith(MockitoExtension.class)
public class EmailControllerTest {

    RestTestClient client;

    @InjectMocks
    private EmailController emailController;

    @BeforeEach
    public void setup() {
        client = RestTestClient.bindToController(emailController).build();
    }

    @Test
    void postWithBodyReturnsAccepted() {
        Map<String, String> payload = Map.of(
                "to", "user@example.com",
                "subject", "Test",
                "message", "message",
                "payloadUri", "htttp://cdn.mailpayload.de");

        client.post()
                .uri("/v1/email")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .exchange()
                .expectStatus().isAccepted();
    }

    @Test
    void postWithBodyWithoutPayloadUriReturnsAccepted() {
        Map<String, String> payload = Map.of(
                "to", "user@example.com",
                "message", "message",
                "subject", "Test");

        client.post()
                .uri("/v1/email")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .exchange()
                .expectStatus().isAccepted();
    }

    @Test
    void postWithoutBodyReturnsBadRequest() {
        client.post()
              .uri("/v1/email")
              .exchange()
              .expectStatus().isBadRequest();
    }

}
