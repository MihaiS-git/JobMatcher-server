package com.jobmatcher.server.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class RecoverPasswordRequest {
    private String email;
}
