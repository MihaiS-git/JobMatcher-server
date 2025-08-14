package com.jobmatcher.server.service;

import com.jobmatcher.server.domain.Address;
import com.jobmatcher.server.domain.User;
import com.jobmatcher.server.exception.ResourceNotFoundException;
import com.jobmatcher.server.mapper.UserMapper;
import com.jobmatcher.server.model.AddressRequestDTO;
import com.jobmatcher.server.model.UserRequestDTO;
import com.jobmatcher.server.model.UserResponseDTO;
import com.jobmatcher.server.repository.AddressRepository;
import com.jobmatcher.server.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class UserServiceImpl implements IUserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final AddressRepository addressRepository;

    public UserServiceImpl(
            UserRepository userRepository,
            UserMapper userMapper,
            AddressRepository addressRepository
    ) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.addressRepository = addressRepository;
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() ->
                new ResourceNotFoundException("User not found."));
    }

    @Override
    public UserResponseDTO getUserById(UUID id) {
        User user = userRepository.findByIdWithAddress(id).orElseThrow(() ->
                new ResourceNotFoundException("User not found."));
        return userMapper.toUserResponseDto(user);
    }

    @Transactional
    @Override
    public UserResponseDTO updateUserById(UUID id, UserRequestDTO userRequest) {
        User existentUser = userRepository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("User not found."));

        if(userRequest.getRole() != null) {
            existentUser.setRole(userRequest.getRole());
        }
        if(userRequest.getAccountNonExpired() != null) {
            existentUser.setAccountNonExpired(userRequest.getAccountNonExpired());
        }
        if(userRequest.getAccountNonLocked() != null) {
            existentUser.setAccountNonLocked(userRequest.getAccountNonLocked());
        }
        if(userRequest.getCredentialsNonExpired() != null) {
            existentUser.setCredentialsNonExpired(userRequest.getCredentialsNonExpired());
        }
        if(userRequest.getEnabled() != null) {
            existentUser.setEnabled(userRequest.getEnabled());
        }
        if(userRequest.getPhone() != null) {
            existentUser.setPhone(userRequest.getPhone());
        }
        if(userRequest.getFirstName() != null) {
            existentUser.setFirstName(userRequest.getFirstName());
        }
        if(userRequest.getLastName() != null) {
            existentUser.setLastName(userRequest.getLastName());
        }
        if(userRequest.getPictureUrl() != null) {
            existentUser.setPictureUrl(userRequest.getPictureUrl());
        }

        User updatedUser = userRepository.save(existentUser);

        return userMapper.toUserResponseDto(updatedUser);
    }

    @Transactional
    @Override
    public UserResponseDTO updateAddressByUserId(UUID userId, AddressRequestDTO addressRequest) {
        User existentUser = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found."));
        Address address = existentUser.getAddress();

        if(address == null){
            address = new Address();
        }

        if (addressRequest.getStreet() != null) {
            address.setStreet(addressRequest.getStreet());
        }
        if (addressRequest.getCity() != null) {
            address.setCity(addressRequest.getCity());
        }
        if (addressRequest.getState() != null) {
            address.setState(addressRequest.getState());
        }
        if (addressRequest.getPostalCode() != null) {
            address.setPostalCode(addressRequest.getPostalCode());
        }
        if (addressRequest.getCountry() != null) {
            address.setCountry(addressRequest.getCountry());
        }

        Address savedAddress = addressRepository.save(address);

        existentUser.setAddress(savedAddress);
        User savedUser = userRepository.save(existentUser);

        return userMapper.toUserResponseDto(savedUser);
    }
}
