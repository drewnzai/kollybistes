package com.kollybistes.core.kafka;

import com.kollybistes.common.models.NotificationEmail;
import lombok.AllArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class NotificationProducer {
    private final KafkaTemplate<String, NotificationEmail> kafkaTemplate;

    public void sendMail(NotificationEmail notificationEmail){
        kafkaTemplate.send("notification-emails", notificationEmail);
    }
}
