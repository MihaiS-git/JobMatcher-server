package com.jobmatcher.server.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class ResetPasswordRequest {
    private String token;
    private String password;
}
