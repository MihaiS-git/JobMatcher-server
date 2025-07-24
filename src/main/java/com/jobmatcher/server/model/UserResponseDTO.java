package com.jobmatcher.server.model;

import com.jobmatcher.server.domain.Role;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserResponseDTO {
    private String id;
    private String email;
    private Role role;
    private boolean accountNonExpired;
    private boolean accountNonLocked;
    private boolean credentialsNonExpired;
    private boolean enabled;
    private AddressResponseDTO addressResponseDto;
    private String phone;
    private String firstName;
    private String lastName;
    private String pictureUrl;
}
