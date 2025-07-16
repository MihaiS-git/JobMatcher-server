package com.jobmatcher.server.controller;

import com.jobmatcher.server.exception.PasswordRecoveryException;
import com.jobmatcher.server.exception.InvalidAuthException;
import com.jobmatcher.server.model.*;
import com.jobmatcher.server.service.AuthenticationService;
import com.jobmatcher.server.service.IRefreshTokenService;
import com.jobmatcher.server.service.PasswordRecoveryService;
import com.jobmatcher.server.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static com.jobmatcher.server.model.ApiConstants.API_VERSION;

@RestController
@RequestMapping(value = API_VERSION + "/auth")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final RegistrationService registrationService;
    private final PasswordRecoveryService passwordRecoveryService;
    private final IRefreshTokenService refreshTokenService;

    public AuthController(
            AuthenticationService authenticationService,
            RegistrationService registrationService,
            PasswordRecoveryService passwordRecoveryService,
            IRefreshTokenService refreshTokenService
    ) {
        this.authenticationService = authenticationService;
        this.registrationService = registrationService;
        this.passwordRecoveryService = passwordRecoveryService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<SuccessResponse> register(@Valid @RequestBody RegisterRequest request) {
        registrationService.register(request);
        return ResponseEntity.ok().body(new SuccessResponse(true));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthenticationRequest request) {
        AuthResponse authResponse = authenticationService.login(request);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/recover-password")
    public ResponseEntity<SuccessResponse> recoverPassword(@RequestBody RecoverPasswordRequest request) {
        boolean success = passwordRecoveryService.recoverPassword(request);
        if (!success) {
            throw new PasswordRecoveryException("No account found with this email.");
        }
        return ResponseEntity.ok().body(new SuccessResponse(true));
    }

    @GetMapping("/validate-reset-token")
    public ResponseEntity<GenericResponse> validateResetToken(@RequestParam String token) {
        boolean valid = passwordRecoveryService.validateResetToken(token);
        if (!valid) {
            throw new InvalidAuthException("Invalid or expired reset token.");
        }
        return ResponseEntity.ok(new GenericResponse(true, "Token is valid."));
    }

    @PutMapping("/reset-password")
    public ResponseEntity<SuccessResponse> resetPassword(@RequestBody ResetPasswordRequest request) {
        passwordRecoveryService.resetPassword(request);
        return ResponseEntity.ok().body(new SuccessResponse(true));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthUserDTO> getCurrentUser(@AuthenticationPrincipal Object principal) {
        AuthUserDTO authUserDTO = authenticationService.getAuthUserFromPrincipal(principal);
        return ResponseEntity.ok(authUserDTO);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody RefreshTokenRequest refreshTokenRequest) {
        AuthResponse authResponse = refreshTokenService.refreshToken(refreshTokenRequest.getRefreshToken());
        return ResponseEntity.ok(authResponse);
    }
}
