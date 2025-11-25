package de.kombinat9f.emailclient.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.kombinat9f.emailclient.service.EmailService;

@RestController
@RequestMapping("/api/email")
public class EmailController {

    private EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping
    public ResponseEntity<HttpStatus> sendSingleEmail(@RequestBody Map<String, String> body) {
        if (body.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        // later change to fill kafka topic method and move call of sendOneEmail to kafka listener area
        boolean success = emailService.sendOneEmail(body);
        if (!success) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

}
