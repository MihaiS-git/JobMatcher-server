package com.jobmatcher.server.controller;

import com.jobmatcher.server.model.CustomerDetailDTO;
import com.jobmatcher.server.model.CustomerProfileRequestDTO;
import com.jobmatcher.server.service.ICustomerProfileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

import static com.jobmatcher.server.model.ApiConstants.API_VERSION;
@Slf4j
@RestController
@RequestMapping(API_VERSION + "/profiles/customers")
public class CustomerProfileController {

    private final ICustomerProfileService customerProfileService;

    public CustomerProfileController(ICustomerProfileService customerProfileService) {
        this.customerProfileService = customerProfileService;
    }

    @GetMapping(path = "/{id}")
    public ResponseEntity<CustomerDetailDTO> getCustomerProfileById(@PathVariable UUID id){
        CustomerDetailDTO customerProfileDto =  customerProfileService.getCustomerProfileById(id);
        return ResponseEntity.ok(customerProfileDto);
    }

    @GetMapping(path = "/users/{userId}")
    public ResponseEntity<CustomerDetailDTO> getCustomerByUserId(@PathVariable UUID userId) {
        CustomerDetailDTO profile = customerProfileService.getCustomerProfileByUserId(userId);
        return ResponseEntity.ok(profile);
    }

    @PostMapping
    public ResponseEntity<CustomerDetailDTO> saveCustomerProfile(@RequestBody CustomerProfileRequestDTO request){
        CustomerDetailDTO savedProfile = customerProfileService.saveCustomerProfile(request);
        URI location = URI.create(API_VERSION + "/profiles/customers/" + savedProfile.getProfileId());
        return ResponseEntity.created(location).body(savedProfile);
    }

    @PatchMapping(path="/update/{id}")
    public ResponseEntity<CustomerDetailDTO> updateCustomerProfileById(
            @PathVariable UUID id,
            @RequestBody CustomerProfileRequestDTO request
    ){
        CustomerDetailDTO updatedProfile = customerProfileService.updateCustomerProfile(id, request);
        return ResponseEntity.ok(updatedProfile);
    }

}
