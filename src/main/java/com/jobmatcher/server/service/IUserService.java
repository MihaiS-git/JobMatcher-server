package com.jobmatcher.server.service;

import com.jobmatcher.server.domain.User;
import com.jobmatcher.server.model.AddressRequestDTO;
import com.jobmatcher.server.model.UserRequestDTO;
import com.jobmatcher.server.model.UserResponseDTO;
import jakarta.validation.Valid;

import java.util.UUID;

public interface IUserService {

    User getUserByEmail(String email);

    UserResponseDTO getUserById(UUID id);

    UserResponseDTO updateUserById(String userId, @Valid UserRequestDTO userRequest);

    UserResponseDTO updateAddressByUserId(UUID userId, @Valid AddressRequestDTO addressRequest);
}
