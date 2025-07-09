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
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .pictureUrl(user.getPictureUrl())
                .build();
    }
}
