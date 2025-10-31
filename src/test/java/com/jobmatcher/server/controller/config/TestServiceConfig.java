package com.jobmatcher.server.controller.config;

import com.cloudinary.Cloudinary;
import com.google.api.services.gmail.Gmail;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestServiceConfig {
    // Mock Gmail instead of building a real one
    @Bean
    public Gmail gmail() {
        return mock(Gmail.class);
    }

    @Bean
    public Cloudinary cloudinary() {
        return mock(Cloudinary.class);
    }

    @Bean
    public com.stripe.Stripe stripe() {
        return mock(com.stripe.Stripe.class);
    }

    @Bean
    public com.stripe.net.Webhook webhook() {
        return mock(com.stripe.net.Webhook.class);
    }

    @Bean
    public com.stripe.model.checkout.Session session() {
        return mock(com.stripe.model.checkout.Session.class);
    }

}
