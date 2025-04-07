package com.kollybistes.notification.services;


import com.kollybistes.common.models.NotificationEmail;
import lombok.AllArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@AllArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    private String build(String message) {
        Context context = new Context();
        context.setVariable("message", message);
        return templateEngine.process("MailTemplate", context);
    }


    public void sendMail(NotificationEmail notificationEmail) throws Exception {
//        MimeMessagePreparator messagePreparator = mimeMessage -> {
//            MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage);
//            messageHelper.setFrom("kollybistes@email.com");
//            messageHelper.setTo(notificationEmail.getRecipient());
//            messageHelper.setSubject(notificationEmail.getSubject());
//            messageHelper.setText(build(notificationEmail.getBody()));
//        };
//        try {
//            mailSender.send(messagePreparator);
//        } catch (MailException e) {
//            throw new Exception("Exception occurred when sending mail to " + notificationEmail.getRecipient(), e);
//        }
        System.out.println(notificationEmail);
    }
}
