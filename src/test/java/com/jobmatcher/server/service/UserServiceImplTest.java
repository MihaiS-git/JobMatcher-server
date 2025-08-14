package com.jobmatcher.server.service;

import com.jobmatcher.server.domain.Address;
import com.jobmatcher.server.domain.Role;
import com.jobmatcher.server.exception.ResourceNotFoundException;
import com.jobmatcher.server.mapper.UserMapper;
import com.jobmatcher.server.model.AddressRequestDTO;
import com.jobmatcher.server.model.UserRequestDTO;
import com.jobmatcher.server.model.UserResponseDTO;
import com.jobmatcher.server.repository.AddressRepository;
import com.jobmatcher.server.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.jobmatcher.server.domain.User;


import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    UserRepository userRepository;

    @Mock
    AddressRepository addressRepository;

    @Mock
    UserMapper userMapper;

    @InjectMocks
    UserServiceImpl userService;

    User sampleUser;
    UUID userId;

    @BeforeEach
    void setup(){
        userId = UUID.randomUUID();

        sampleUser = new User();
        sampleUser.setEmail("test@example.com");
        sampleUser.setPassword("encodedPassword");
        sampleUser.setEnabled(true);
        sampleUser.setAccountNonExpired(true);
        sampleUser.setAccountNonLocked(true);
        sampleUser.setCredentialsNonExpired(true);
        sampleUser.setRole(Role.valueOf("CUSTOMER"));
    }

    @Test
    void getUserByEmail_existingUser_returnsUser() {
        String email = "test@example.com";

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(sampleUser));

        User result = userService.getUserByEmail(email);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(email);

        verify(userRepository).findByEmail(email);
    }

    @Test
    void getUserByEmail_userNotFound_throwsResourceNotFoundException() {
        String email = "notfound@example.com";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserByEmail(email))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(userRepository).findByEmail(email);
    }

    @Test
    void getUserById_existingUser_returnsUserResponseDto() {
        UserResponseDTO responseDto = UserResponseDTO.builder().build();
        when(userRepository.findByIdWithAddress(userId)).thenReturn(Optional.of(sampleUser));
        when(userMapper.toUserResponseDto(sampleUser)).thenReturn(responseDto);

        UserResponseDTO result = userService.getUserById(userId);

        assertThat(result).isEqualTo(responseDto);
        verify(userRepository).findByIdWithAddress(userId);
        verify(userMapper).toUserResponseDto(sampleUser);
    }

    @Test
    void getUserById_userNotFound_throwsResourceNotFoundException() {
        when(userRepository.findByIdWithAddress(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(userRepository).findByIdWithAddress(userId);
    }

    @Test
    void updateUserById_updatesFieldsCorrectlyAndReturnsDto() {
        UserRequestDTO request = UserRequestDTO.builder()
                .role(Role.ADMIN)
                .accountNonExpired(false)
                .accountNonLocked(false)
                .credentialsNonExpired(false)
                .enabled(false)
                .phone("123456789")
                .firstName("John")
                .lastName("Doe")
                .pictureUrl("http://image.url")
                .build();

        User updatedUser = new User();
        UserResponseDTO responseDto = UserResponseDTO.builder().build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);
        when(userMapper.toUserResponseDto(updatedUser)).thenReturn(responseDto);

        UserResponseDTO result = userService.updateUserById(userId, request);

        assertThat(result).isEqualTo(responseDto);
        assertThat(sampleUser.getRole()).isEqualTo(Role.ADMIN);
        assertThat(sampleUser.isAccountNonExpired()).isFalse();
        assertThat(sampleUser.isAccountNonLocked()).isFalse();
        assertThat(sampleUser.isCredentialsNonExpired()).isFalse();
        assertThat(sampleUser.isEnabled()).isFalse();
        assertThat(sampleUser.getPhone()).isEqualTo("123456789");
        assertThat(sampleUser.getFirstName()).isEqualTo("John");
        assertThat(sampleUser.getLastName()).isEqualTo("Doe");
        assertThat(sampleUser.getPictureUrl()).isEqualTo("http://image.url");

        verify(userRepository).findById(userId);
        verify(userRepository).save(sampleUser);
        verify(userMapper).toUserResponseDto(updatedUser);
    }

    @Test
    void updateUserById_userNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        UserRequestDTO request = UserRequestDTO.builder().build();

        assertThatThrownBy(() -> userService.updateUserById(userId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(userRepository).findById(userId);
    }

    @Test
    void updateUserById_allFieldsNull_doesNotUpdateAnything() {
        UserRequestDTO request = UserRequestDTO.builder().build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(sampleUser)).thenReturn(sampleUser);
        when(userMapper.toUserResponseDto(sampleUser)).thenReturn(UserResponseDTO.builder().build());

        UserResponseDTO result = userService.updateUserById(userId, request);

        assertThat(result).isNotNull();
        verify(userRepository).save(sampleUser);
    }


    @Test
    void updateAddressByUserId_existingAddress_updatesAndReturnsDto() {
        Address existingAddress = new Address();
        existingAddress.setStreet("Old Street");

        sampleUser.setAddress(existingAddress);

        AddressRequestDTO addressRequest = AddressRequestDTO.builder()
                .street("New Street")
                .city("New City")
                .state("New State")
                .postalCode("12345")
                .country("New Country")
                .build();

        Address savedAddress = new Address();
        User savedUser = new User();
        UserResponseDTO responseDto = UserResponseDTO.builder().build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(addressRepository.save(any(Address.class))).thenReturn(savedAddress);
        when(userRepository.save(sampleUser)).thenReturn(savedUser);
        when(userMapper.toUserResponseDto(savedUser)).thenReturn(responseDto);

        UserResponseDTO result = userService.updateAddressByUserId(userId, addressRequest);

        assertThat(result).isEqualTo(responseDto);
        assertThat(existingAddress.getStreet()).isEqualTo("New Street");
        assertThat(existingAddress.getCity()).isEqualTo("New City");
        assertThat(existingAddress.getState()).isEqualTo("New State");
        assertThat(existingAddress.getPostalCode()).isEqualTo("12345");
        assertThat(existingAddress.getCountry()).isEqualTo("New Country");

        verify(userRepository).findById(userId);
        verify(addressRepository).save(existingAddress);
        verify(userRepository).save(sampleUser);
        verify(userMapper).toUserResponseDto(savedUser);
    }

    @Test
    void updateAddressByUserId_noExistingAddress_createsAndReturnsDto() {
        sampleUser.setAddress(null);

        AddressRequestDTO addressRequest = AddressRequestDTO.builder()
                .street("Street")
                .city("City")
                .state("State")
                .postalCode("54321")
                .country("Country")
                .build();

        Address newAddress = new Address();
        User savedUser = new User();
        UserResponseDTO responseDto = UserResponseDTO.builder().build();

        // To capture the Address passed to save() to assert fields
        ArgumentCaptor<Address> addressCaptor = ArgumentCaptor.forClass(Address.class);

        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(addressRepository.save(addressCaptor.capture())).thenReturn(newAddress);
        when(userRepository.save(sampleUser)).thenReturn(savedUser);
        when(userMapper.toUserResponseDto(savedUser)).thenReturn(responseDto);

        UserResponseDTO result = userService.updateAddressByUserId(userId, addressRequest);

        assertThat(result).isEqualTo(responseDto);

        Address captured = addressCaptor.getValue();
        assertThat(captured.getStreet()).isEqualTo("Street");
        assertThat(captured.getCity()).isEqualTo("City");
        assertThat(captured.getState()).isEqualTo("State");
        assertThat(captured.getPostalCode()).isEqualTo("54321");
        assertThat(captured.getCountry()).isEqualTo("Country");

        verify(userRepository).findById(userId);
        verify(addressRepository).save(any(Address.class));
        verify(userRepository).save(sampleUser);
        verify(userMapper).toUserResponseDto(savedUser);
    }

    @Test
    void updateAddressByUserId_userNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        AddressRequestDTO addressRequest = AddressRequestDTO.builder().build();

        assertThatThrownBy(() -> userService.updateAddressByUserId(userId, addressRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(userRepository).findById(userId);
    }

    @Test
    void updateAddressByUserId_existingAddress_someFieldsNull_updatesOnlyNonNull() {
        Address existingAddress = new Address();
        existingAddress.setStreet("old street");
        sampleUser.setAddress(existingAddress);

        AddressRequestDTO request = AddressRequestDTO.builder()
                .city("New City")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(addressRepository.save(existingAddress)).thenReturn(existingAddress);
        when(userRepository.save(sampleUser)).thenReturn(sampleUser);
        when(userMapper.toUserResponseDto(sampleUser)).thenReturn(UserResponseDTO.builder().build());

        UserResponseDTO result = userService.updateAddressByUserId(userId, request);

        assertThat(result).isNotNull();
        assertThat(existingAddress.getCity()).isEqualTo("New City");
        verify(addressRepository).save(existingAddress);
    }

    @Test
    void updateAddressByUserId_allFieldsNull_doesNotModifyAddress() {
        sampleUser.setAddress(new Address());

        AddressRequestDTO request = AddressRequestDTO.builder().build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(addressRepository.save(any(Address.class))).thenReturn(sampleUser.getAddress());
        when(userRepository.save(sampleUser)).thenReturn(sampleUser);
        when(userMapper.toUserResponseDto(sampleUser)).thenReturn(UserResponseDTO.builder().build());

        UserResponseDTO result = userService.updateAddressByUserId(userId, request);

        assertThat(result).isNotNull();
        verify(addressRepository).save(any(Address.class));
    }

    @Test
    void updateAddressByUserId_addressIsNull_createsNewAddress() {
        sampleUser.setAddress(null);
        AddressRequestDTO request = AddressRequestDTO.builder()
                .postalCode("54321")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(sampleUser)).thenReturn(sampleUser);
        when(userMapper.toUserResponseDto(sampleUser)).thenReturn(UserResponseDTO.builder().build());

        UserResponseDTO result = userService.updateAddressByUserId(userId, request);

        assertThat(result).isNotNull();
        assertThat(sampleUser.getAddress().getPostalCode()).isEqualTo("54321");
    }

}