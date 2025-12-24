//package com.jobmatcher.server.service;
//
//import com.jobmatcher.server.domain.RefreshToken;
//import com.jobmatcher.server.domain.Role;
//import com.jobmatcher.server.domain.User;
//import com.jobmatcher.server.exception.InvalidAuthException;
//import com.jobmatcher.server.mapper.UserMapper;
//import com.jobmatcher.server.model.AuthResponse;
//import com.jobmatcher.server.model.AuthUserDTO;
//import com.jobmatcher.server.model.AuthenticationRequest;
//import com.jobmatcher.server.repository.UserRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.oauth2.core.user.OAuth2User;
//
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class AuthenticationServiceTest {
//
//    @Mock
//    UserRepository userRepository;
//
//    @Mock
//    PasswordEncoder passwordEncoder;
//
//    @Mock
//    JwtService jwtService;
//
//    @Mock
//    IRefreshTokenService refreshTokenService;
//
//    @Mock
//    IUserService userService;
//
//    @Mock
//    UserMapper userMapper;
//
//    @InjectMocks
//    AuthenticationService authenticationService;
//
//    User sampleUser;
//    AuthUserDTO sampleDto;
//
//    @BeforeEach
//    void setup() {
//        sampleUser = new User();
//        sampleUser.setEmail("user@example.com");
//        sampleUser.setPassword("encodedPassword");
//        sampleUser.setEnabled(true);
//        sampleUser.setAccountNonExpired(true);
//        sampleUser.setAccountNonLocked(true);
//        sampleUser.setCredentialsNonExpired(true);
//        sampleUser.setRole(Role.valueOf("CUSTOMER"));
//
//        sampleDto = AuthUserDTO.builder()
//                .email("test@example.com")
//                .role("CUSTOMER")
//                .build();
//    }
//
//    @Test
//    void authenticate_shouldReturnJwtToken_whenCredentialsAreValid() {
//        AuthenticationRequest request = new AuthenticationRequest();
//        request.setEmail("user@example.com");
//        request.setPassword("plainPassword");
//
//        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(sampleUser));
//        when(passwordEncoder.matches(request.getPassword(), sampleUser.getPassword())).thenReturn(true);
//        when(jwtService.generateToken(sampleUser)).thenReturn("jwt-token");
//
//        String token = authenticationService.authenticate(request);
//
//        assertThat(token).isEqualTo("jwt-token");
//        verify(userRepository, times(1)).findByEmail(request.getEmail());
//        verify(passwordEncoder, times(1)).matches(request.getPassword(), sampleUser.getPassword());
//        verify(jwtService, times(1)).generateToken(sampleUser);
//    }
//
//    @Test
//    void authenticate_shouldThrowInvalidAuthException_whenUserNotFound() {
//        AuthenticationRequest request = new AuthenticationRequest();
//        request.setEmail("missing@example.com");
//        request.setPassword("plainPassword");
//
//        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
//
//        InvalidAuthException exception = assertThrows(InvalidAuthException.class, () -> {
//            authenticationService.authenticate(request);
//        });
//
//        assertThat(exception.getMessage()).isEqualTo("Invalid email or password");
//        verify(userRepository, times(1)).findByEmail(request.getEmail());
//    }
//
//    @Test
//    void authenticate_shouldThrowInvalidAuthException_whenPasswordDoesNotMatch() {
//        AuthenticationRequest request = new AuthenticationRequest();
//        request.setEmail("user@example.com");
//        request.setPassword("wrongPassword");
//
//        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(sampleUser));
//        when(passwordEncoder.matches(request.getPassword(), sampleUser.getPassword())).thenReturn(false);
//
//        InvalidAuthException exception = assertThrows(InvalidAuthException.class, () -> {
//            authenticationService.authenticate(request);
//        });
//
//        assertThat(exception.getMessage()).isEqualTo("Invalid email or password");
//    }
//
//    @Test
//    void authenticate_shouldThrowInvalidAuthException_whenAccountDisabled() {
//        sampleUser.setEnabled(false);
//        AuthenticationRequest request = new AuthenticationRequest();
//        request.setEmail("user@example.com");
//        request.setPassword("password");
//
//        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(sampleUser));
//        when(passwordEncoder.matches(request.getPassword(), sampleUser.getPassword())).thenReturn(true);
//
//        InvalidAuthException exception = assertThrows(InvalidAuthException.class, () -> {
//            authenticationService.authenticate(request);
//        });
//
//        assertThat(exception.getMessage()).isEqualTo("Account is disabled");
//    }
//
//    @Test
//    void authenticate_shouldThrowInvalidAuthException_whenAccountLocked() {
//        sampleUser.setAccountNonLocked(false);
//        AuthenticationRequest request = new AuthenticationRequest();
//        request.setEmail("user@example.com");
//        request.setPassword("password");
//
//        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(sampleUser));
//        when(passwordEncoder.matches(request.getPassword(), sampleUser.getPassword())).thenReturn(true);
//
//        InvalidAuthException exception = assertThrows(InvalidAuthException.class, () -> {
//            authenticationService.authenticate(request);
//        });
//
//        assertThat(exception.getMessage()).isEqualTo("Account is locked");
//    }
//
//    @Test
//    void authenticate_shouldThrowInvalidAuthException_whenAccountExpired() {
//        sampleUser.setAccountNonExpired(false);
//        AuthenticationRequest request = new AuthenticationRequest();
//        request.setEmail("user@example.com");
//        request.setPassword("password");
//
//        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(sampleUser));
//        when(passwordEncoder.matches(request.getPassword(), sampleUser.getPassword())).thenReturn(true);
//
//        InvalidAuthException exception = assertThrows(InvalidAuthException.class, () -> {
//            authenticationService.authenticate(request);
//        });
//
//        assertThat(exception.getMessage()).isEqualTo("Account is expired");
//    }
//
//    @Test
//    void authenticate_shouldThrowInvalidAuthException_whenCredentialsExpired() {
//        sampleUser.setCredentialsNonExpired(false);
//        AuthenticationRequest request = new AuthenticationRequest();
//        request.setEmail("user@example.com");
//        request.setPassword("password");
//
//        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(sampleUser));
//        when(passwordEncoder.matches(request.getPassword(), sampleUser.getPassword())).thenReturn(true);
//
//        InvalidAuthException exception = assertThrows(InvalidAuthException.class, () -> {
//            authenticationService.authenticate(request);
//        });
//
//        assertThat(exception.getMessage()).isEqualTo("Credentials are expired");
//    }
//
//    @Test
//    void login_shouldReturnAuthResponse() {
//        AuthenticationRequest request = new AuthenticationRequest();
//        request.setEmail("user@example.com");
//        request.setPassword("password");
//        RefreshToken mockRefreshToken = new RefreshToken();
//        mockRefreshToken.setToken("refresh-token");
//
//
//        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(sampleUser));
//        when(passwordEncoder.matches(request.getPassword(), sampleUser.getPassword())).thenReturn(true);
//        when(jwtService.generateToken(sampleUser)).thenReturn("jwt-token");
//        when(userService.getUserByEmail("user@example.com")).thenReturn(sampleUser);
//        doNothing().when(refreshTokenService).deleteByUser(sampleUser);
//        when(refreshTokenService.createRefreshToken(sampleUser)).thenReturn(mockRefreshToken);
//
//        when(userMapper.toDto(sampleUser)).thenReturn(sampleDto);
//
//        AuthResponse response = authenticationService.login(request);
//
//        assertThat(response.getToken()).isEqualTo("jwt-token");
//        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
//        verify(refreshTokenService, times(1)).deleteByUser(sampleUser);
//        verify(refreshTokenService, times(1)).createRefreshToken(sampleUser);
//    }
//
//    @Test
//    void login_shouldThrow_whenAuthenticationFails() {
//        AuthenticationRequest request = new AuthenticationRequest();
//        request.setEmail("missing@example.com");
//        request.setPassword("password");
//        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
//
//        assertThatThrownBy(() -> authenticationService.login(request))
//                .isInstanceOf(InvalidAuthException.class)
//                .hasMessage("Invalid email or password");
//    }
//
//    @Test
//    void getAuthUserFromPrincipal_shouldReturnDto_whenPrincipalIsOAuth2User() {
//        OAuth2User oAuth2User = mock(OAuth2User.class);
//
//        when(oAuth2User.getAttribute("email")).thenReturn("test@example.com");
//        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));
//        when(userMapper.toDto(sampleUser)).thenReturn(sampleDto);
//
//        AuthUserDTO result = authenticationService.getAuthUserFromPrincipal(oAuth2User);
//
//        assertThat(result).isEqualTo(sampleDto);
//        verify(oAuth2User).getAttribute("email");
//        verify(userRepository).findByEmail("test@example.com");
//        verify(userMapper).toDto(sampleUser);
//    }
//
//    @Test
//    void getAuthUserFromPrincipal_shouldThrow_whenOAuth2EmailIsMissing() {
//        OAuth2User oAuth2User = mock(OAuth2User.class);
//        when(oAuth2User.getAttribute("email")).thenReturn(null);
//
//        assertThatThrownBy(() -> authenticationService.getAuthUserFromPrincipal(oAuth2User))
//                .isInstanceOf(InvalidAuthException.class)
//                .hasMessage("Email missing in OAuth2 principal");
//    }
//
//    @Test
//    void getAuthUserFromPrincipal_shouldReturnDto_whenPrincipalIsUser() {
//        User userPrincipal = mock(User.class);
//
//        when(userPrincipal.getEmail()).thenReturn("test@example.com");
//        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));
//        when(userMapper.toDto(sampleUser)).thenReturn(sampleDto);
//
//        AuthUserDTO result = authenticationService.getAuthUserFromPrincipal(userPrincipal);
//
//        assertThat(result).isEqualTo(sampleDto);
//        verify(userPrincipal).getEmail();
//        verify(userRepository).findByEmail("test@example.com");
//        verify(userMapper).toDto(sampleUser);
//    }
//
//    @Test
//    void getAuthUserFromPrincipal_shouldThrow_whenPrincipalIsUnsupportedType() {
//        Object unsupportedPrincipal = new Object();
//
//        assertThatThrownBy(() -> authenticationService.getAuthUserFromPrincipal(unsupportedPrincipal))
//                .isInstanceOf(InvalidAuthException.class)
//                .hasMessage("Unsupported authentication principal type");
//    }
//
//    @Test
//    void getAuthUserFromPrincipal_shouldThrow_whenUserNotFound() {
//        User userPrincipal = mock(User.class);
//        when(userPrincipal.getEmail()).thenReturn("test@example.com");
//        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
//
//        assertThatThrownBy(() -> authenticationService.getAuthUserFromPrincipal(userPrincipal))
//                .isInstanceOf(InvalidAuthException.class)
//                .hasMessage("User not found");
//    }
//}