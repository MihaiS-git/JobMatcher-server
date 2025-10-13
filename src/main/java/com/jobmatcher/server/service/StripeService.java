//package com.jobmatcher.server.service;
//
//import com.jobmatcher.server.domain.Invoice;
//import com.stripe.Stripe;
//import com.stripe.model.*;
//import com.stripe.model.checkout.Session;
//import com.stripe.param.checkout.SessionCreateParams;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//import java.math.BigDecimal;
//
//@Slf4j
//@Service
//public class StripeService {
//
//    @Value("${stripe.api.key}")
//    private String stripeKey;
//
//    @Value("${frontend.url.dev}")
//    private String frontendUrl;
//
//    public String createCheckoutSession(Invoice invoice) {
//        Stripe.apiKey = stripeKey;
//
//        BigDecimal amount = invoice.getAmount();
//        String successUrl = frontendUrl + "/invoice-success?invoiceId=" + invoice.getId();
//        String cancelUrl = frontendUrl + "/invoice-cancel?invoiceId=" + invoice.getId();
//
//        SessionCreateParams.LineItem.PriceData.ProductData productData =
//                SessionCreateParams.LineItem.PriceData.ProductData.builder()
//                        .setName("Invoice " + invoice.getId())
//                        .setDescription(invoice.getContract() == null ? "" : invoice.getContract().getDescription())
//                        .build();
//
//        SessionCreateParams.LineItem.PriceData priceData =
//                SessionCreateParams.LineItem.PriceData.builder()
//                        .setCurrency("usd")
//                        .setUnitAmount(amount.multiply(BigDecimal.valueOf(100)).longValue()) // cents
//                        .setProductData(productData)
//                        .build();
//
//        SessionCreateParams.LineItem lineItem =
//                SessionCreateParams.LineItem.builder()
//                        .setPriceData(priceData)
//                        .setQuantity(1L)
//                        .build();
//
//        SessionCreateParams params = SessionCreateParams.builder()
//                .setMode(SessionCreateParams.Mode.PAYMENT)
//                .setSuccessUrl(successUrl)
//                .setCancelUrl(cancelUrl)
//                .setClientReferenceId(invoice.getId().toString())
//                .addLineItem(lineItem)
//                .build();
//
//        try {
//            Session session = Session.create(params);
//            return session.getUrl();
//        } catch (Exception e) {
//            throw new RuntimeException("Stripe session creation failed", e);
//        }
//    }
//}
