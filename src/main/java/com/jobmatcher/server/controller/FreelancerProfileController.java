package com.jobmatcher.server.controller;

import com.jobmatcher.server.model.FreelancerDetailDTO;
import com.jobmatcher.server.model.FreelancerProfileRequestDTO;
import com.jobmatcher.server.model.FreelancerSummaryDTO;
import com.jobmatcher.server.service.IFreelancerProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Set;
import java.util.UUID;

import static com.jobmatcher.server.model.ApiConstants.API_VERSION;

@RestController
@RequestMapping(API_VERSION + "/profiles/freelancers")
public class FreelancerProfileController {

    private final IFreelancerProfileService freelancerProfileService;

    public FreelancerProfileController(IFreelancerProfileService freelancerProfileService) {
        this.freelancerProfileService = freelancerProfileService;
    }

    @GetMapping
    public ResponseEntity<Set<FreelancerSummaryDTO>> getAllFreelancers() {
        Set<FreelancerSummaryDTO> profiles = freelancerProfileService.getAllFreelancerProfiles();
        return ResponseEntity.ok(profiles);
    }

    @GetMapping(path = "/{id}")
    public ResponseEntity<FreelancerDetailDTO> getFreelancerById(@PathVariable UUID id) {
        FreelancerDetailDTO profile = freelancerProfileService.getFreelancerProfileById(id);
        return ResponseEntity.ok(profile);
    }

    @GetMapping(path = "/users/{userId}")
    public ResponseEntity<FreelancerDetailDTO> getFreelancerByUserId(@PathVariable UUID userId) {
        FreelancerDetailDTO profile = freelancerProfileService.getFreelancerProfileByUserId(userId);
        return ResponseEntity.ok(profile);
    }

    @PostMapping
    public ResponseEntity<FreelancerDetailDTO> saveFreelancerProfile(@RequestBody @Valid FreelancerProfileRequestDTO request) {
        FreelancerDetailDTO savedProfile = freelancerProfileService.saveFreelancerProfile(request);
        URI location = URI.create(API_VERSION + "/profiles/freelancers/" + savedProfile.getProfileId());
        return ResponseEntity.created(location).body(savedProfile);
    }

    @PatchMapping(path = "/update/{id}")
    public ResponseEntity<FreelancerDetailDTO> updateFreelancerProfileById(
            @PathVariable UUID id,
            @RequestBody @Valid FreelancerProfileRequestDTO request
    ) {
        FreelancerDetailDTO updatedProfile = freelancerProfileService.updateFreelancerProfile(id, request);
        return ResponseEntity.ok(updatedProfile);
    }
}
