package de.kombinat9f.emailclient.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import de.kombinat9f.emailclient.controller.AttachmentClient;

import org.springframework.mail.SimpleMailMessage;

@ExtendWith(MockitoExtension.class)
class EmailServiceRetryTest {

    @Mock
    JavaMailSender mailSender;

    @Mock
    AttachmentClient attachmentClient;

    @Test
    void sendSimpleEmail_retriesAndSucceedsAfter4Failures() {
        EmailService emailService = new EmailService(mailSender, attachmentClient);

        AtomicInteger counter = new AtomicInteger(0);
        doAnswer(invocation -> {
            if (counter.getAndIncrement() < 4) { // first 4 attempts throw
                throw new MailSendException("temporary failure");
            }
            return null; // succeed on 5th attempt
        }).when(mailSender).send(any(SimpleMailMessage.class));

        Map<String, String> content = Map.of(
                "to", "user@example.com",
                "subject", "RetryTest",
                "message", "Hello"
        );

        // sendOneEmail catches final exception, but in this case it should succeed eventually
        emailService.sendOneEmail(content);

        // expect 5 attempts (4 failures + 1 success)
        verify(mailSender, times(5)).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendSimpleEmail_retriesAndFailsAfterMaxAttempts() {
        EmailService emailService = new EmailService(mailSender, attachmentClient);

        doThrow(new MailSendException("permanent failure")).when(mailSender).send(any(SimpleMailMessage.class));

        Map<String, String> content = Map.of(
                "to", "user@example.com",
                "subject", "RetryTest",
                "message", "Hello"
        );

        // sendOneEmail should swallow the exception after retries, not throw
        emailService.sendOneEmail(content);

        // expect 6 attempts (initial + 5 retries)
        verify(mailSender, times(6)).send(any(SimpleMailMessage.class));
    }
}
