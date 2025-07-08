package com.jobmatcher.server.model;

import lombok.Getter;

@Getter
public class RegisterRequest {
    private String email;
    private String password;

    public RegisterRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }
}
