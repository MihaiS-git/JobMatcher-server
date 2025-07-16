package com.jobmatcher.server.service;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;

import com.jobmatcher.server.domain.User;
import com.jobmatcher.server.exception.EmailSendException;
import com.jobmatcher.server.exception.GmailApiException;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;

@Slf4j
@Service
public class GmailSender {

    @Value("${frontend.base.url}")
    private String FRONTEND_BASE_URL;

    @Value("${gmail.send.max-retries:3}")
    private int MAX_RETRIES;

    private static final String GMAIL_USER = "me";

    private final Gmail gmail;

    public GmailSender(Gmail gmail) {
        this.gmail = gmail;
    }

    public void sendResetEmail(User user, String token) {
        try {
            String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
            String resetUrl = "%s/reset-password?token=%s".formatted(FRONTEND_BASE_URL, encodedToken);
            sendEmail(user.getEmail(), resetUrl);
        } catch (MessagingException | IOException e) {
            log.error("Failed to create or encode email", e);
            throw new EmailSendException("Email preparation failed", e);
        } catch (GmailApiException e) {
            throw e;
        }
    }

    private void sendEmail(String to, String resetUrl) throws MessagingException, IOException {
        MimeMessage email = createEmail(to, GMAIL_USER, resetUrl);
        Message message = createMessageWithEmail(email);

        int attempts = 0;
        int maxRetries = MAX_RETRIES;

        while (true) {
            try {
                gmail.users().messages().send(GMAIL_USER, message).execute();
                log.info("Email sent successfully to {}", to);
                break;
            } catch (GoogleJsonResponseException e) {
                int code = e.getDetails().getCode();
                String reason = e.getDetails().getMessage();

                log.error("Gmail API error ({}): {}", code, reason);

                if ((code == 429 || (code >= 500 && code < 600)) && attempts < maxRetries) {
                    attempts++;
                    log.info("Retrying sending email, attempt {}", attempts + 1);
                    try {
                        Thread.sleep(1000); // wait 1 second before retry
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new EmailSendException("Interrupted during retry wait", ie);
                    }
                    continue;
                }
                throw new GmailApiException(reason, code);
            } catch (Exception ex) {
                log.error("Unexpected error when sending email", ex);
                throw new EmailSendException("Unexpected failure when sending email.", ex);
            }
        }
    }


    private MimeMessage createEmail(String to, String from, String resetUrl)
            throws MessagingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        String subject = "JobMatcher reset password";

        String bodyText = """
                    <p>Hello,</p>
                    <p>Click the following link to reset your JobMatcher password:</p>
                    <p><a href="%s">
                    Reset Password</a></p>
                    <p>If you didn't request this, please ignore.</p>
                    <p>Thanks,<br/>JobMatcher Team</p>
                """.formatted(resetUrl);

        MimeMessage email = new MimeMessage(session);
        email.setFrom(new InternetAddress(from));
        email.addRecipient(jakarta.mail.Message.RecipientType.TO,
                new InternetAddress(to));
        email.setSubject(subject);
        email.setContent(bodyText, "text/html; charset=utf-8");
        return email;
    }

    private Message createMessageWithEmail(MimeMessage email) throws MessagingException, IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        email.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }
}