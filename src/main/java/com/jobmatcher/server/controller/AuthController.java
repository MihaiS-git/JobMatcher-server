package com.jobmatcher.server.controller;

import com.jobmatcher.server.domain.RefreshToken;
import com.jobmatcher.server.domain.User;
import com.jobmatcher.server.exception.InvalidAuthException;
import com.jobmatcher.server.model.*;
import com.jobmatcher.server.service.AuthenticationService;
import com.jobmatcher.server.service.IRefreshTokenService;
import com.jobmatcher.server.service.JwtService;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping(value="/api/v0/auth")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final IRefreshTokenService refreshTokenService;
    private final JwtService jwtService;

    public AuthController(AuthenticationService authenticationService, IRefreshTokenService refreshTokenService, JwtService jwtService) {
        this.authenticationService = authenticationService;
        this.refreshTokenService = refreshTokenService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody RegisterRequest request){
        authenticationService.register(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthenticationRequest request){
        AuthResponse authResponse = authenticationService.login(request);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/recover-password")
    @Transactional
    public ResponseEntity<?> recoverPassword(@RequestBody RecoverPasswordRequest request) {
        boolean success = authenticationService.recoverPassword(request);
        if(!success){
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(
                            404,
                            "Not Found",
                            "No account found with this email.",
                            "/recover-password",
                            LocalDateTime.now().toString())
                    );

        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/validate-reset-token")
    public ResponseEntity<?> validateResetToken(@RequestParam String token){
        boolean valid = authenticationService.validateResetToken(token);
        if (!valid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new GenericResponse(false, "Invalid or expired token."));
        }
        return ResponseEntity.ok(new GenericResponse(true, "Token is valid."));
    }

    @PutMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request){
        authenticationService.resetPassword(request);
        return ResponseEntity.ok().body("{\"success\":true}");
    }

    @GetMapping("/me")
    public ResponseEntity<AuthUserDTO> getCurrentUser(@AuthenticationPrincipal Object principal) {
        if (principal == null) {
            throw new InvalidAuthException("User not authenticated");
        }

        AuthUserDTO userDTO = switch (principal) {
            case OAuth2User oAuth2User -> AuthUserDTO.builder()
                    .email(oAuth2User.getAttribute("email"))
                    .firstName(oAuth2User.getAttribute("name"))
                    .lastName(oAuth2User.getAttribute("family_name"))
                    .role("ROLE_USER")
                    .pictureUrl(oAuth2User.getAttribute("picture"))
                    .build();
            case User user -> AuthUserDTO.builder()
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .role(user.getRole().name())
                    .pictureUrl(user.getPictureUrl())
                    .build();
            default -> throw new InvalidAuthException("Unsupported authentication type");
        };

        return ResponseEntity.ok(userDTO);
    }

    @Transactional
    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody RefreshTokenRequest refreshTokenRequest) {
        if (!refreshTokenService.validateRefreshToken(refreshTokenRequest.getRefreshToken())) {
            throw new InvalidAuthException("Invalid or expired refresh token");
        }

        RefreshToken oldToken = refreshTokenService.findByToken(refreshTokenRequest.getRefreshToken())
                .orElseThrow(() -> new InvalidAuthException("Refresh token not found"));

        User user = oldToken.getUser();

        // Delete the old token
        refreshTokenService.deleteRefreshToken(refreshTokenRequest.getRefreshToken());

        // Generate new token
        String newAccessToken = jwtService.generateToken(user);
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user);

        AuthUserDTO userDTO = AuthUserDTO.builder()
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .pictureUrl(user.getPictureUrl())
                .build();

        return ResponseEntity.ok(new AuthResponse(newAccessToken, newRefreshToken.getToken(), userDTO));
    }
}
