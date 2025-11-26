package de.kombinat9f.emailclient.service;

import de.kombinat9f.emailclient.controller.AttachmentClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailServiceTest {

    @Mock
    JavaMailSender mailSender;

    @Mock
    AttachmentClient restClient;

    @InjectMocks
    EmailService emailService;

    @Test
    void sendOneEmail_sendsEmail() {
        Map<String, String> content = Map.of(
            "to", "user@example.com",
            "subject", "Greetings",
            "message", "Hello!"
        );

        emailService.sendOneEmail(content);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();
        assertArrayEquals(new String[] {"user@example.com"}, sent.getTo());
        assertEquals("Greetings", sent.getSubject());
        assertEquals("Hello!", sent.getText());
    }

    @Test
    void sendOneEmail_invalidEmail_doesNotSendEmail() {
        Map<String, String> content = Map.of(
            "to", "bad-email",
            "subject", "Hi",
            "message", "Body"
        );

        emailService.sendOneEmail(content);
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendOneEmail_missingSubject_doesNotSendEmail() {
        Map<String, String> content = Map.of(
            "to", "user@example.com",
            "message", "Body"
        );

        emailService.sendOneEmail(content);
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendOneEmail_missingMessage_doesNotSendEmail() {
        Map<String, String> content = Map.of(
            "to", "user@example.com",
            "subject", "Hi"
        );

        emailService.sendOneEmail(content);
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendEmailWithAttachment_shouldPrepareAttachmentAndSend(@TempDir Path tempDir) throws Exception {
        // Arrange
        // create a lightweight MimeMessage backed by a simple Session
        Session session = Session.getInstance(new Properties());
        MimeMessage mimeMessage = new MimeMessage(session);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // create temp file and make restClient return a FileInputStream for it
        Path tempFile = tempDir.resolve("attachment.bin");
        Files.write(tempFile, "hello-attachment".getBytes());
        byte[] bytes = Files.readAllBytes(tempFile);
        when(restClient.prepareAttachmentFile(ArgumentMatchers.any(URI.class)))
                .thenReturn(bytes);

        EmailService emailService = new EmailService(mailSender, restClient);

        Map<String, String> payload = new HashMap<>();
        payload.put("to", "user@example.com");
        payload.put("subject", "Test");
        payload.put("message", "Body");
        payload.put("payloadUri", "http://example.com/file");
        payload.put("dataType", "application/octet-stream");

        // Act
        emailService.sendOneEmail(payload);

        // Assert - verify the attachment was prepared and the mail was sent
        verify(restClient, times(1)).prepareAttachmentFile(ArgumentMatchers.any(URI.class));
        verify(mailSender, times(1)).send(mimeMessage);
    }
}
