package com.jobmatcher.server.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String refreshToken;
    private AuthUserDTO user;
}
