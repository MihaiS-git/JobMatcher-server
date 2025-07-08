package com.jobmatcher.server.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserDTO {
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private String pictureUrl;
}
