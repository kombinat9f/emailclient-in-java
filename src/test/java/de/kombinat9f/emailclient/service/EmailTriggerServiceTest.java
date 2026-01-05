package de.kombinat9f.emailclient.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.kombinat9f.emailclient.domain.EmailRequest;

@ExtendWith(MockitoExtension.class)
class EmailTriggerServiceTest {

	@Mock
	private KafkaEmailTriggerProducer producer;

	@InjectMocks
	private EmailTriggerService service;

	@Captor
	private ArgumentCaptor<EmailRequest> requestCaptor;

	@Test
	void produceEmailTrigger_validInput_callsProducerWithEmailRequest() throws Exception {
		Map<String, String> payload = new HashMap<>();
		payload.put("to", "user@example.com");
		payload.put("subject", "Hello");
		payload.put("message", "This is a test");

		service.produceEmailTrigger(payload);

		verify(producer, times(1)).sendEmailRequest(requestCaptor.capture());
		EmailRequest captured = requestCaptor.getValue();
		assertNotNull(captured);

		assertEquals("user@example.com", captured.getEmailAddress());
		assertEquals("Hello", captured.getSubject());
		assertEquals("This is a test", captured.getMessage());

		// payloadUri and dataType should be Optional.empty() because not provided
		Object payloadUri = captured.getPayloadUri();
		if (payloadUri instanceof Optional) {
			assertFalse(((Optional<?>) payloadUri).isPresent());
		}
		Object dataType = captured.getDataType();
		if (dataType instanceof Optional) {
			assertFalse(((Optional<?>) dataType).isPresent());
		}
	}

	@Test
	void produceEmailTrigger_validInputWithPayload_callsProducerWithOptionalFields() throws Exception {
		Map<String, String> payload = new HashMap<>();
		payload.put("to", "user@example.com");
		payload.put("subject", "Hello");
		payload.put("message", "This is a test");
		payload.put("payloadUri", "http://example.com/data.json");
		payload.put("dataType", "application/json");

		service.produceEmailTrigger(payload);

		verify(producer, times(1)).sendEmailRequest(requestCaptor.capture());
		EmailRequest captured = requestCaptor.getValue();
		assertNotNull(captured);

		Object payloadUri = captured.getPayloadUri();
		assertEquals("http://example.com/data.json", payloadUri);

		Object dataType = captured.getDataType();
		assertEquals("application/json", dataType);
	}

	@Test
	void produceEmailTrigger_invalidEmail_noCallToProducer() {
		Map<String, String> payload = new HashMap<>();
		payload.put("to", "invalid-email");
		payload.put("subject", "Hello");
		payload.put("message", "This is a test");

		service.produceEmailTrigger(payload);

		verify(producer, times(0)).sendEmailRequest(isNull());
	}

	@Test
	void produceEmailTrigger_missingSubject_noCallToProducer() {
		Map<String, String> payload = new HashMap<>();
		payload.put("to", "user@example.com");
		// no subject
		payload.put("message", "This is a test");

		service.produceEmailTrigger(payload);

		verify(producer, times(0)).sendEmailRequest(isNull());
	}

	@Test
	void produceEmailTrigger_missingMessage_noCallToProducer() {
		Map<String, String> payload = new HashMap<>();
		payload.put("to", "user@example.com");
		payload.put("subject", "Hello");
		// no message

		service.produceEmailTrigger(payload);
		verify(producer, times(0)).sendEmailRequest(isNull());
	}

	
}