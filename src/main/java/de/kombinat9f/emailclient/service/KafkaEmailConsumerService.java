package de.kombinat9f.emailclient.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import de.kombinat9f.emailclient.domain.EmailRequest;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class KafkaEmailConsumerService {

    private final EmailSenderService emailSenderService;

    @KafkaListener(topics = "email-requests", groupId = "email-service-group")
    public void listen(String emailJson) {
        
        EmailRequest emailRequest = convertFromJson(emailJson);

        emailSenderService.sendOneEmail(emailRequest);
    }

    private EmailRequest convertFromJson(String emailJson) {
        
        return new ObjectMapper().readValue(emailJson, EmailRequest.class);
    }

}
