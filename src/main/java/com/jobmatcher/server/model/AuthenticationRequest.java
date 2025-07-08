package com.jobmatcher.server.model;

import lombok.Getter;

@Getter
public class AuthenticationRequest {
    private String email;
    private String password;
}
