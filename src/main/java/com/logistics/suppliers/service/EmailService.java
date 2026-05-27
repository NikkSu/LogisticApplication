package com.logistics.suppliers.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("forbootmail@gmail.com");
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            System.out.println("Письмо успешно отправлено на " + to);
        } catch (Exception e) {
            System.err.println("ОШИБКА SMTP: " + e.getMessage());
            throw new RuntimeException("Ошибка почтового сервера: " + e.getMessage());
        }
    }
}