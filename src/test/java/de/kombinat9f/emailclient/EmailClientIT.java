package de.kombinat9f.emailclient;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.internet.MimeMessage;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:test_application.properties")
@EmbeddedKafka(partitions = 3, topics = { "test-emails-topic" })
class EmailClientIT {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Autowired
    private JavaMailSender mailSender;

    private GreenMail greenMail;

    @BeforeEach
    void startGreenMail() {
        // start SMTP server on default test port (3025)
        greenMail = new GreenMail(ServerSetupTest.SMTP);
        greenMail.start();

        // If the application-provided JavaMailSender tries to authenticate, disable it for tests
        if (mailSender instanceof JavaMailSenderImpl) {
            JavaMailSenderImpl impl = (JavaMailSenderImpl) mailSender;
            impl.setHost("localhost");
            impl.setPort(3025);
            impl.setUsername(null);
            impl.setPassword(null);
            impl.getJavaMailProperties().put("mail.smtp.auth", "false");
            impl.getJavaMailProperties().put("mail.smtp.starttls.enable", "false");
        }
    }

    @AfterEach
    void stopGreenMail() {
        if (greenMail != null) {
            greenMail.stop();
        }
    }

    @Test
    void mailIntegrationTest_sendsEmailToGreenMail() throws Exception {
        // prepare message
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo("recipient@example.com");
        msg.setFrom("default@origin.com");
        msg.setSubject("Integration test");
        msg.setText("Hello from integration test");

        // send using Spring's JavaMailSender (configured from test properties)
        mailSender.send(msg);

        // wait for incoming email
        boolean arrived = greenMail.waitForIncomingEmail(5000, 1);
        assertTrue(arrived, "Expected 1 email to arrive at GreenMail");

        MimeMessage[] received = greenMail.getReceivedMessages();
        assertEquals(1, received.length);
        MimeMessage m = received[0];
        assertEquals("Integration test", m.getSubject());
        assertTrue(m.getAllRecipients()[0].toString().contains("recipient@example.com"));
    }

    @Test
    void kafkaEmbeddedIntegrationTest_producerSendsAndConsumerReceives() {
        String topic = "test-emails-topic";
        String key = "key1";
        String value = "{\"to\":\"recipient@example.com\",\"subject\":\"Hello\",\"body\":\"hi\"}";

        // create producer properties pointing to embedded broker
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafka.getBrokersAsString());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.ACKS_CONFIG, "all");

        // use simple producer via KafkaProducer wrapper provided by Spring Kafka test utilities
        org.apache.kafka.clients.producer.KafkaProducer<String, String> producer =
                new org.apache.kafka.clients.producer.KafkaProducer<>(producerProps);

        // send record
        producer.send(new ProducerRecord<>(topic, key, value));
        producer.flush();
        producer.close();

        // create consumer properties pointing to embedded broker
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafka.getBrokersAsString());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-group");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        Consumer<String, String> consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(Collections.singletonList(topic));

        // poll for the record
        ConsumerRecord<String, String> polled = null;
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            var records = consumer.poll(Duration.ofMillis(200));
            if (!records.isEmpty()) {
                polled = records.iterator().next();
                break;
            }
        }
        consumer.close();

        assertNotNull(polled, "Expected to receive a Kafka record from embedded broker");
        assertEquals(key, polled.key());
        assertEquals(value, polled.value());
    }
}