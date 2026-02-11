package de.kombinat9f.emailclient.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import de.kombinat9f.emailclient.domain.EmailRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaEmailConsumerService {

    private final EmailSenderService emailSenderService;

    @KafkaListener(topics = "email-requests", groupId = "email-service-group")
    public void listen(String emailJson) {
        
        EmailRequest emailRequest = convertFromJson(emailJson);

        emailSenderService.sendOneEmail(emailRequest);
        log.info("Emailrequest read from queue");
    }

    private EmailRequest convertFromJson(String emailJson) {
        
        return new ObjectMapper().readValue(emailJson, EmailRequest.class);
    }

}
