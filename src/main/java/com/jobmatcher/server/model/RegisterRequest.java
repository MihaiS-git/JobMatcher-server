package com.jobmatcher.server.model;

import com.jobmatcher.server.domain.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class RegisterRequest {

    @NotBlank
    @Pattern(
            regexp = "^[a-zA-Z0-9]+([._%+-]?[a-zA-Z0-9]+)*@[a-zA-Z0-9-]+(\\.[a-zA-Z]{2,})+$",
            message = "Please enter a valid email."
    )
    @Schema(example = "user01@jobmatcher.com")
    private String email;

    @NotBlank
    @Size(min=10, message="Password must be at least 10 characters")
    @Schema(example = "Password!23")
    private String password;

    @NotBlank
    @Size(min=2, message="First name must be at least 2 characters")
    @Schema(example = "Jane")
    private String firstName;

    @NotBlank
    @Size(min=2, message="Last name must be at least 2 characters")
    @Schema(example = "Doe")
    private String lastName;

    @NotNull
    @Schema(example = "STAFF")
    private Role role;
}
