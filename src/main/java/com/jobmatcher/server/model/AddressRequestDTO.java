package com.jobmatcher.server.model;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AddressRequestDTO {

    @Size(min = 3, message = "Street must be at least 3 characters long.")
    private String street;

    @Size(min = 2, message = "City must be at least 2 characters.")
    private String city;

    @Size(min = 2, message = "State must be at least 2 characters.")
    @Pattern(
            regexp = "^[\\p{L} .'-]{2,50}$",
            message = "Invalid state format."
    )
    private String state;

    @Pattern(
            regexp = "^[A-Za-z0-9][A-Za-z0-9\\s-]{3,}$",
            message = "Invalid postal code format."
    )
    private String postalCode;

    @Size(min = 2, message = "Country must be at least 2 characters.")
    @Pattern(
            regexp = "^[\\p{L} .'-]{2,50}$",
            message = "Invalid country format."
    )
    private String country;

}
