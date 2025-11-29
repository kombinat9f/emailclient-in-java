package de.kombinat9f.emailclient.controller;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import de.kombinat9f.emailclient.service.EmailTriggerService;

@ExtendWith(MockitoExtension.class)
public class EmailControllerTest {

    RestTestClient client;

    @Mock
    private EmailTriggerService emailService;

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
                "message", "message",
                "subject", "Test");

        client.post()
                .uri("/api/email")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .exchange()
                .expectStatus().isAccepted();
    }

    @Test
    void postWithoutBodyReturnsBadRequest() {
        client.post()
              .uri("/api/email")
              .exchange()
              .expectStatus().isBadRequest();
    }

}
