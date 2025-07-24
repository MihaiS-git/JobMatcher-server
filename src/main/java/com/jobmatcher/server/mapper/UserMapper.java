package com.jobmatcher.server.mapper;

import com.jobmatcher.server.domain.Address;
import com.jobmatcher.server.domain.User;
import com.jobmatcher.server.model.AddressResponseDTO;
import com.jobmatcher.server.model.AuthUserDTO;
import com.jobmatcher.server.model.UserResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    private final AddressMapper addressMapper;

    public UserMapper(AddressMapper addressMapper) {
        this.addressMapper = addressMapper;
    }

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

    public UserResponseDTO toUserResponseDto(User user){
        if(user == null) return null;

        Address address = user.getAddress();
        AddressResponseDTO addressResponseDTO = addressMapper.toDto(address);

        return UserResponseDTO.builder()
                .id(user.getId().toString())
                .email(user.getEmail())
                .role(user.getRole())
                .accountNonExpired(user.isAccountNonExpired())
                .accountNonLocked(user.isAccountNonLocked())
                .credentialsNonExpired(user.isCredentialsNonExpired())
                .enabled(user.isEnabled())
                .addressResponseDto(addressResponseDTO)
                .phone(user.getPhone())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .pictureUrl(user.getPictureUrl())
                .build();
    }
}
