package com.jobmatcher.server.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthUserDTO {

    private UUID id;
    private String email;

    private String role;

    @Builder.Default
    private String firstName = "unknown";

    @Builder.Default
    private String lastName = "unknown";

    @Builder.Default
    private String pictureUrl = "unknown";

}
