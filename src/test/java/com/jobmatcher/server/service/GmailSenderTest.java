package com.jobmatcher.server.service;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.jobmatcher.server.domain.User;
import com.jobmatcher.server.exception.EmailSendException;
import com.jobmatcher.server.exception.GmailApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GmailSenderTest {

    @Mock
    Gmail gmail;
    @Mock
    Gmail.Users users;
    @Mock
    Gmail.Users.Messages messages;
    @Mock
    Gmail.Users.Messages.Send send;

    @InjectMocks
    GmailSender gmailSender;

    final String userEmail = "user@example.com";
    final String resetToken = "token123";

    User sampleUser;

    @BeforeEach
    void setup() throws Exception {
        // Inject the private fields using reflection helper (TestUtils assumed available)
        TestUtils.setField(gmailSender, "FRONTEND_BASE_URL", "http://frontend.test");
        TestUtils.setField(gmailSender, "MAX_RETRIES", 2);

        when(gmail.users()).thenReturn(users);
        when(users.messages()).thenReturn(messages);
        when(messages.send(eq("me"), any(Message.class))).thenReturn(send);

        sampleUser = new User();
        sampleUser.setEmail(userEmail);
    }

    @Test
    void sendResetEmail_success() throws Exception {
        when(send.execute()).thenReturn(new Message());

        gmailSender.sendResetEmail(sampleUser, resetToken);

        verify(send).execute();
    }

    @Test
    void sendResetEmail_retriesOnRetryableGmailApiException() throws Exception {
        GoogleJsonResponseException retryableException = createGoogleJsonResponseException(429, "Rate Limit");

        when(send.execute())
                .thenThrow(retryableException)
                .thenReturn(new Message());

        gmailSender.sendResetEmail(sampleUser, resetToken);

        verify(send, times(2)).execute();
    }

    @Test
    void sendResetEmail_throwsGmailApiException_whenNonRetryable() throws Exception {
        GoogleJsonResponseException nonRetryableException = createGoogleJsonResponseException(400, "Bad Request");

        when(send.execute()).thenThrow(nonRetryableException);

        assertThatThrownBy(() -> gmailSender.sendResetEmail(sampleUser, resetToken))
                .isInstanceOf(GmailApiException.class)
                .hasMessageContaining("Bad Request");

        verify(send).execute();
    }

    @Test
    void sendResetEmail_throwsOnInterruptedDuringRetry() throws Exception {
        GoogleJsonResponseException retryableException = createGoogleJsonResponseException(429, "Rate Limit");

        when(send.execute())
                .thenThrow(retryableException)
                .thenAnswer(invocation -> {
                    throw new InterruptedException("Interrupted for test");
                });

        assertThatThrownBy(() -> gmailSender.sendResetEmail(sampleUser, resetToken))
                .isInstanceOf(EmailSendException.class)
                .hasMessageContaining("Unexpected failure when sending email.");
    }

    @Test
    void sendResetEmail_throwsOnUnexpectedException() throws Exception {
        when(send.execute()).thenThrow(new RuntimeException("Boom"));

        assertThatThrownBy(() -> gmailSender.sendResetEmail(sampleUser, resetToken))
                .isInstanceOf(EmailSendException.class)
                .hasMessageContaining("Unexpected failure");
    }

    // Helper method to mock GoogleJsonResponseException with code and message
    private GoogleJsonResponseException createGoogleJsonResponseException(int code, String message) {
        GoogleJsonResponseException ex = mock(GoogleJsonResponseException.class);
        com.google.api.client.googleapis.json.GoogleJsonError error = mock(com.google.api.client.googleapis.json.GoogleJsonError.class);

        lenient().when(error.getCode()).thenReturn(code);
        lenient().when(error.getMessage()).thenReturn(message);

        when(ex.getStatusCode()).thenReturn(code);
        when(ex.getMessage()).thenReturn(message);
        when(ex.getDetails()).thenReturn(error);

        return ex;
    }
}
