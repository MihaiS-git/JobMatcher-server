package com.jobmatcher.server.service;

import com.jobmatcher.server.config.AppProperties;
import com.jobmatcher.server.domain.RefreshToken;
import com.jobmatcher.server.domain.User;
import com.jobmatcher.server.exception.*;
import com.jobmatcher.server.mapper.UserMapper;
import com.jobmatcher.server.model.*;
import com.jobmatcher.server.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

@Slf4j
@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final IRefreshTokenService refreshTokenService;
    private final IUserService userService;
    private final UserMapper userMapper;
    private final AppProperties appProperties;

    public AuthenticationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            IRefreshTokenService refreshTokenService,
            IUserService userService,
            UserMapper userMapper, AppProperties appProperties
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userService = userService;
        this.userMapper = userMapper;
        this.appProperties = appProperties;
    }

    private static final Set<String> DEMO_USERS = Set.of(
            "user0@jobmatcher.com",
            "user1@jobmatcher.com"
    );

    public String authenticate(AuthenticationRequest request) {
        log.info("User {} is attempting to authenticate.", request.getEmail());

        if (appProperties.demoMode() && !DEMO_USERS.contains(request.getEmail())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only demo accounts are allowed"
            );
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidAuthException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Bad credentials for email: {}", request.getEmail());
            throw new InvalidAuthException("Invalid email or password");
        }

        if (!user.isEnabled()) {
            log.warn("Account is disabled for email: {}", request.getEmail());
            throw new InvalidAuthException("Account is disabled");
        }

        if (!user.isAccountNonLocked()) {
            log.warn("Account is locked for email: {}", request.getEmail());
            throw new InvalidAuthException("Account is locked");
        }

        if (!user.isAccountNonExpired()) {
            log.warn("Account is expired for email: {}", request.getEmail());
            throw new InvalidAuthException("Account is expired");
        }

        if (!user.isCredentialsNonExpired()) {
            log.warn("Credentials are expired for email: {}", request.getEmail());
            throw new InvalidAuthException("Credentials are expired");
        }

        log.info("User {} authenticated successfully with role {}", user.getEmail(), user.getRole());
        return jwtService.generateToken(user);
    }

    @Transactional
    public AuthResponse login(AuthenticationRequest request) {
        String jwtToken = authenticate(request);
        User user = userService.getUserByEmail(request.getEmail());
        refreshTokenService.deleteByUser(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
        return new AuthResponse(jwtToken, refreshToken.getToken(), userMapper.toDto(user));
    }

    public AuthUserDTO getAuthUserFromPrincipal(Object principal) {
        String email = extractEmailFromPrincipal(principal);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidAuthException("User not found"));

        return userMapper.toDto(user);
    }

    private String extractEmailFromPrincipal(Object principal) {
        if (principal instanceof OAuth2User oAuth2User) {
            String email = oAuth2User.getAttribute("email");
            if (email == null) throw new InvalidAuthException("Email missing in OAuth2 principal");
            return email;
        } else if (principal instanceof User user) {
            return user.getEmail();
        } else {
            throw new InvalidAuthException("Unsupported authentication principal type");
        }
    }
}
