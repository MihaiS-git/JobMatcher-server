package com.jobmatcher.server.model;

import lombok.*;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class AuthUserDTO {

    private UUID id;
    private String email;
    private String role;
    private String firstName;
    private String lastName;
    private String pictureUrl;

}
