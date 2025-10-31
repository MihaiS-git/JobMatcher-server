package com.jobmatcher.server.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@RequiredArgsConstructor
public class AuthenticationRequest {

    @NotBlank
    @Pattern(
            regexp = "^[a-zA-Z0-9]+([._%+-]?[a-zA-Z0-9]+)*@[a-zA-Z0-9-]+(\\.[a-zA-Z]{2,})+$",
            message = "Please enter a valid email."
    )
    private String email;

    @NotBlank
    @Size(min=10, message="Password must be at least 10 characters")
    private String password;
}
