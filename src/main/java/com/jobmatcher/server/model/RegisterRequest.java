package com.jobmatcher.server.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class RegisterRequest {

    @NotBlank
    @Pattern(
            regexp = "^[a-zA-Z0-9]+([._%+-]?[a-zA-Z0-9]+)*@[a-zA-Z0-9-]+(\\.[a-zA-Z]{2,})+$",
            message = "Please enter a valid email."
    )
    private String email;

    @NotBlank
    @Size(min=10, message="Password must be at least 10 characters")
    private String password;

    @NotBlank
    @Size(min=2, message="First name must be at least 2 characters")
    private String firstName;

    @NotBlank
    @Size(min=2, message="Last name must be at least 2 characters")
    private String lastName;
}
