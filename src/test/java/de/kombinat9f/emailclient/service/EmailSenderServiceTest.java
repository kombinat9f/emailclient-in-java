package de.kombinat9f.emailclient.service;

import de.kombinat9f.emailclient.controller.AttachmentClient;
import de.kombinat9f.emailclient.domain.EmailRequest;

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
import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailSenderServiceTest {

    @Mock
    JavaMailSender mailSender;

    @Mock
    AttachmentClient restClient;

    @InjectMocks
    EmailSenderService emailService;

    @Test
    void sendOneEmail_sendsEmail() {
        EmailRequest emailRequest = EmailRequest.builder().emailAddress("user@example.com").subject("Greetings").message("Hello!").build();

        emailService.sendOneEmail(emailRequest);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();
        assertArrayEquals(new String[] {"user@example.com"}, sent.getTo());
        assertEquals("Greetings", sent.getSubject());
        assertEquals("Hello!", sent.getText());
    }

    @Test
    void sendEmailWithAttachment_shouldPrepareAttachmentAndSend(@TempDir Path tempDir) throws Exception {

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

        EmailSenderService emailService = new EmailSenderService(mailSender, restClient);


        EmailRequest emailRequest = EmailRequest.builder()
            .emailAddress("user@example.com")
            .subject("Test")
            .message("Body")
            .payloadUri(Optional.of("http://example.com/file"))
            .dataType(Optional.of("application/octet-stream")).build();

        emailService.sendOneEmail(emailRequest);

        verify(restClient, times(1)).prepareAttachmentFile(ArgumentMatchers.any(URI.class));
        verify(mailSender, times(1)).send(mimeMessage);
    }
}
