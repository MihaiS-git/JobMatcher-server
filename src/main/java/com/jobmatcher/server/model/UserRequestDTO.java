package com.jobmatcher.server.model;

import com.jobmatcher.server.domain.Role;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.URL;

@Getter
@Setter
@Builder
public class UserRequestDTO {

    private Role role;
    private Boolean accountNonExpired;
    private Boolean accountNonLocked;
    private Boolean credentialsNonExpired;
    private Boolean enabled;

    @Pattern(
            regexp = "^(\\+?\\d{1,3}[-.\\s]?)?(\\(?\\d{3}\\)?[-.\\s]?)?\\d{3}[-.\\s]?\\d{4}$",
            message = "Invalid phone number format."
    )
    private String phone;

    @Size(min=2, message="First name must be at least 2 characters")
    @Pattern(regexp = "^[\\p{L} .'-]{2,50}$", message = "Invalid first name format.")
    private String firstName;

    @Size(min=2, message="Last name must be at least 2 characters")
    @Pattern(regexp = "^[\\p{L} .'-]{2,50}$", message = "Invalid last name format.")
    private String lastName;

    @URL(message = "Invalid picture URL format.")
    private String pictureUrl;
}
