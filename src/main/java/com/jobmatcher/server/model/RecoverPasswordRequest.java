package com.jobmatcher.server.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class RecoverPasswordRequest {
    private String email;
}
