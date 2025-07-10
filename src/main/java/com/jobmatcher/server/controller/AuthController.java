package com.jobmatcher.server.controller;

import com.jobmatcher.server.model.AuthResponse;
import com.jobmatcher.server.model.AuthenticationRequest;
import com.jobmatcher.server.model.RegisterRequest;
import com.jobmatcher.server.service.AuthenticationService;
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
}
