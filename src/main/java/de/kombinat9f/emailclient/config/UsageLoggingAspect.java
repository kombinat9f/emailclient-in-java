package de.kombinat9f.emailclient.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class UsageLoggingAspect {

    @After(value = "execution(* de.kombinat9f.emailclient.service.KafkaEmailTriggerProducer.sendEmailRequest(..))")
    public void logKafkaTriggerAdded(){
        log.info("Emailrequest added to kafka queue");
    }

    @After(value = "execution(* de.kombinat9f.emailclient.service.EmailSenderService.sendOneEmail(..))")
    public void logEmailSent(){
        log.info("Email message sent");
    }

    @After(value = "execution(* de.kombinat9f.emailclient.service.KafkaEmailConsumerService.listen(..))")
    public void logKafkaMessageWithEmailInformationConsumed(){
        log.info("Email request read from queue");
    }

}
