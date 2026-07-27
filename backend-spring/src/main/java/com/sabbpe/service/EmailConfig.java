package com.sabbpe.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class EmailConfig {

    @Value("${app.email.host:smtppro.zoho.in}")
    private String host;

    @Value("${app.email.port:465}")
    private int port;

    @Value("${app.email.username:}")
    private String username;

    @Value("${app.email.password:}")
    private String password;

    @Bean
    public JavaMailSender javaMailSender() {
        if (username == null || username.isBlank()) {
            return new JavaMailSenderImpl();
        }
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username);
        sender.setPassword(password);

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.ssl.trust", host);
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.debug", "false");

        return sender;
    }
}
