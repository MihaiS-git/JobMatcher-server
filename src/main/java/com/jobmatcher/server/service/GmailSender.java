package com.jobmatcher.server.service;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Properties;

@Slf4j
@Service
public class GmailSender {

    private final Gmail gmail;

    public GmailSender(Gmail gmail) {
        this.gmail = gmail;
    }
    public void sendEmail(String to, String subject, String bodyText) throws Exception {
            MimeMessage email = createEmail(to, "me", subject, bodyText);
            Message message = createMessageWithEmail(email);
        try {
            gmail.users().messages().send("me", message).execute();
        } catch (GoogleJsonResponseException e){
            GoogleJsonError error = e.getDetails();
            if(error.getCode() == 403){
                log.error("Unable to send message: " + e.getDetails());
            } else {
                throw e;
            }
        }
    }

    private MimeMessage createEmail(String to, String from, String subject, String bodyText)
            throws MessagingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);
        email.setFrom(new InternetAddress(from));
        email.addRecipient(jakarta.mail.Message.RecipientType.TO,
                new InternetAddress(to));
        email.setSubject(subject);
        email.setContent(bodyText, "text/html; charset=utf-8");
        return email;
    }

    private Message createMessageWithEmail(MimeMessage email) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        email.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }
}