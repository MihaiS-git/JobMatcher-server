package com.jobmatcher.server.controller;

import com.jobmatcher.server.model.AddressRequestDTO;
import com.jobmatcher.server.model.UserRequestDTO;
import com.jobmatcher.server.model.UserResponseDTO;
import com.jobmatcher.server.service.IUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.jobmatcher.server.model.ApiConstants.API_VERSION;

@RestController
@RequestMapping(value = API_VERSION + "/users")
public class UserController {

    private final IUserService userService;

    public UserController(IUserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable String id){
        UserResponseDTO userResponseDTO = userService.getUserById(UUID.fromString(id));
        return ResponseEntity.ok(userResponseDTO);
    }

    @PatchMapping("/update/{id}")
    public ResponseEntity<UserResponseDTO> updateUserById(
            @PathVariable String id,
            @RequestBody @Valid UserRequestDTO userRequest
    ){
        UserResponseDTO userResponseDTO = userService.updateUserById(UUID.fromString(id), userRequest);
        return ResponseEntity.ok(userResponseDTO);
    }

    @PatchMapping("/update/{id}/address")
    public ResponseEntity<UserResponseDTO> updateAddress(
            @PathVariable String id,
            @RequestBody @Valid AddressRequestDTO addressRequest
    ){
        UserResponseDTO userResponseDTO = userService.updateAddressByUserId(UUID.fromString(id), addressRequest);
        return ResponseEntity.ok(userResponseDTO);
    }

}
