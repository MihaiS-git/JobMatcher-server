package com.jobmatcher.server.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentRequestDTO {

    private String invoiceId;

}
