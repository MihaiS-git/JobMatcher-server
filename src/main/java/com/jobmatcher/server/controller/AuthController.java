package com.jobmatcher.server.controller;

import com.jobmatcher.server.model.*;
import com.jobmatcher.server.service.AuthenticationService;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value="/api/v0/auth")
public class AuthController {

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
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
    public ResponseEntity<Void> recoverPassword(@RequestBody RecoverPasswordRequest request) {
        boolean success = authenticationService.recoverPassword(request);
        if(!success){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
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
}
