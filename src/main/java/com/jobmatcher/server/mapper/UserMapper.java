package com.jobmatcher.server.mapper;

import com.jobmatcher.server.domain.User;
import com.jobmatcher.server.model.AuthUserDTO;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public AuthUserDTO toDto(User user){
        if(user == null){
            return null;
        }

        return AuthUserDTO.builder()
                .id(user.getId() != null ? user.getId() : null)
                .email(user.getEmail())
                .firstName(user.getFirstName() != null ? user.getFirstName() : "unknown")
                .lastName(user.getLastName() != null ? user.getLastName() : "unknown")
                .role(user.getRole().name())
                .pictureUrl(user.getPictureUrl() != null ? user.getPictureUrl() : "unknown")
                .build();
    }
}
