package com.jobmatcher.server.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StripeCheckoutResponseDTO {
    private String url;
}
