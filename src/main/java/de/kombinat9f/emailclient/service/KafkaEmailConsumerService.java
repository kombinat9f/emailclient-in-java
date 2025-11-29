package de.kombinat9f.emailclient.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import de.kombinat9f.emailclient.domain.EmailRequest;
import tools.jackson.databind.ObjectMapper;

@Service
public class KafkaEmailConsumerService {

    private EmailSenderService emailSenderService;

    public KafkaEmailConsumerService(EmailSenderService emailSenderService) {
        this.emailSenderService = emailSenderService;
    }

    @KafkaListener(topics = "email-requests", groupId = "email-service-group")
    public void listen(String emailJson) {
        
        EmailRequest emailRequest = convertFromJson(emailJson);

        emailSenderService.sendOneEmail(emailRequest);
    }

    private EmailRequest convertFromJson(String emailJson) {
        
        return new ObjectMapper().readValue(emailJson, EmailRequest.class);
    }

}
