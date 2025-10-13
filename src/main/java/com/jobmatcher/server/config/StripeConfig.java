//package com.jobmatcher.server.config;
//
//import com.stripe.Stripe;
//import jakarta.annotation.PostConstruct;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class StripeConfig {
//
//    @Value("${stripe.api.key}")
//    private String STRIPE_API_KEY;
//
//    @PostConstruct
//    public void init() {
//        // Initialize Stripe with the API key
//        Stripe.apiKey = STRIPE_API_KEY;
//    }
//}
