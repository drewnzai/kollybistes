package com.kollybistes.notification.kafka;

import com.kollybistes.common.util.NotificationEmail;
import com.kollybistes.notification.services.MailService;
import lombok.AllArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class NotificationConsumer {
    private final MailService mailService;

    @KafkaListener(topics = "notification-emails", groupId = "kollybistes",
            containerFactory = "containerFactory")
    public void consume(@Payload NotificationEmail notificationEmail) throws Exception {
        mailService.sendMail(notificationEmail);
    }
}
