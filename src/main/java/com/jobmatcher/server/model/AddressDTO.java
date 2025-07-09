package com.jobmatcher.server.model;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class AddressDTO {

    private UUID id;
    private String street;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private OffsetDateTime createdAt;
    private OffsetDateTime lastUpdate;

}
