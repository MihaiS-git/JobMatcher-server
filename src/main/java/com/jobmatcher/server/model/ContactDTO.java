package com.jobmatcher.server.model;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContactDTO {

    @NotNull
    private String email;

    @NotNull
    private String phone;

    @NotNull
    private AddressResponseDTO address;
}
