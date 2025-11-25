package de.kombinat9f.emailclient.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailServiceTest {

    @Mock
    JavaMailSender mailSender;

    @InjectMocks
    EmailService emailService;

    @Test
    void sendOneEmail_sendsEmail() {
        Map<String, String> content = Map.of(
            "to", "user@example.com",
            "subject", "Greetings",
            "message", "Hello!"
        );

        boolean response = emailService.sendOneEmail(content);
        assertTrue(response);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();
        assertArrayEquals(new String[] {"user@example.com"}, sent.getTo());
        assertEquals("Greetings", sent.getSubject());
        assertEquals("Hello!", sent.getText());
    }

    @Test
    void sendOneEmail_invalidEmail_returnsFalseAndDoesNotSend() {
        Map<String, String> content = Map.of(
            "to", "bad-email",
            "subject", "Hi",
            "message", "Body"
        );

        boolean response = emailService.sendOneEmail(content);
        assertFalse(response);
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendOneEmail_missingSubject_returnsFalse() {
        Map<String, String> content = Map.of(
            "to", "user@example.com",
            "message", "Body"
        );

        boolean response = emailService.sendOneEmail(content);
        assertFalse(response);
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendOneEmail_missingMessage_returnsFalse() {
        Map<String, String> content = Map.of(
            "to", "user@example.com",
            "subject", "Hi"
        );

        boolean response = emailService.sendOneEmail(content);
        assertFalse(response);
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    // TODO adapt to logic with attachment possibility
    @Test
    void sendOneEmail_withPayloadUri_doesNotSendSimpleEmail_butReturnsTrue() {
        Map<String, String> content = Map.of(
            "to", "user@example.com",
            "subject", "Hi",
            "message", "Body",
            "payloadUri", "http://example.com/payload"
        );

        boolean response = emailService.sendOneEmail(content);
        assertTrue(response);
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }
}
