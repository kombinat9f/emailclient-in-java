# Emailclient in Java #

## Intro ##
This is an example project in Java SDK 25, Springboot 4 and jUnit 6.

It uses a post http controller method, a Restclient to fetch attachments, the new Spring 7 RestTestClient, Spring Kafka and EmbeddedKafka for integration testing, GreenMail for integration testing, Lombok, SWAGGER/OpenAPI, Logging, use of Optionals, Object mapping with Jackson

I implemented the following coding task I found in my old files:

Design and write a MicroService responsible of Asynchronous sending of Emails. It will be used by
various other components to send mail to end customers.

Requirements:
- REST API with synchronous acknowledgment with only one method for sending new Mail
- Mail with attachment should be possible. (Attachment Content will be provided in the request by a
URI pointing to the actual document binaries)
- Queuing until successful response from SMTP Server. Max Retry configurable.
- No Authentication required.
Deliverables
- API Definition using Swagger
- Implementation using Maven/Gradle, Spring Boot, and any other frameworks you may think
useful.

Optional
- We would like to send a large number of mails at certain times. To ensure that the service can
accept and process every request we need a queue in front of the service:
- Implement a consumer who listen and react to a topic in front of the service. To solve this problem
please use Kafka and the Kafka Streams / Consumer-API. The Usage of Avro is a plus but not
necessary.

