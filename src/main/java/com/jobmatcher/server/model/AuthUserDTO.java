package com.jobmatcher.server.model;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class AuthUserDTO {

    private UUID id;
    private String email;
    private String role;
    private String firstName;
    private String lastName;
    private String pictureUrl;

}
