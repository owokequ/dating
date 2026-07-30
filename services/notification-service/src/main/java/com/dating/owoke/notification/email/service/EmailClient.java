package com.dating.owoke.notification.email.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import com.dating.owoke.notification.shared.configuration.NotificationProperties;

@Component
public class EmailClient {

    private final JavaMailSender mailSender;
    private final NotificationProperties properties;

    public EmailClient(JavaMailSender mailSender, NotificationProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    public String send(String recipient, String subject, String body, String actionUrl) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.fromEmail());
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(body + "\n\n" + actionUrl);
        mailSender.send(message);
        return null;
    }
}
