package de.kombinat9f.emailclient.controller;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.kombinat9f.emailclient.service.EmailTriggerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Email Client", description = "Asynchronous API to send an email with optional attachment")
@RestController
@RequestMapping("/api/email")
@Slf4j
@RequiredArgsConstructor
public class EmailController {

    private final EmailTriggerService emailService;

    @Operation(summary = "Send a request to issue an email sending", description = 
                            """
    The body of the post request needs to include all information, where payloadUri for the attachment source and dataType are optional but both needed if an attachment is desired:
    "to":"user@example.com"
    "subject":"Test"
    "message":"Body"
    "payloadUri":"http://example.com/file"
    "dataType":"application/octet-stream"
                            """)
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "If requests could be accepted (execution is not guaranteed)"),
            @ApiResponse(responseCode = "404", description = "If something is wrong with the request, e.g. missing fields or wrong format"),
            @ApiResponse(responseCode = "500")})
    @PostMapping
    public ResponseEntity<HttpStatus> sendSingleEmail(@RequestBody Map<String, String> body) {
        if (body.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        // async, to add an email job to a queue
        CompletableFuture.runAsync(() -> {
            try {
                emailService.produceEmailTrigger(body);
            } catch (Exception e) {
                log.error("Email request could not be started.", e);
            }
        }).exceptionally(ex -> { log.error("Email request could not be started.", ex); return null; });

        return new ResponseEntity<>(HttpStatus.ACCEPTED);

    }
}
