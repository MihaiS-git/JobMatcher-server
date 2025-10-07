package com.jobmatcher.server.controller;

import com.jobmatcher.server.model.MilestoneRequestDTO;
import com.jobmatcher.server.model.MilestoneResponseDTO;
import com.jobmatcher.server.service.IMilestoneService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

import static com.jobmatcher.server.model.ApiConstants.API_VERSION;

@RestController
@RequestMapping(path=API_VERSION + "/milestones")
public class MilestoneController {

    private final IMilestoneService milestoneService;

    public MilestoneController(IMilestoneService milestoneService) {
        this.milestoneService = milestoneService;
    }

    @GetMapping
    public ResponseEntity<Page<MilestoneResponseDTO>> getMilestonesByContractId(
            @RequestParam("contractId") UUID contractId,
            Pageable pageable
            ) {
        Page<MilestoneResponseDTO> milestones = milestoneService.getMilestonesByContractId(contractId, pageable);
        return ResponseEntity.ok(milestones);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MilestoneResponseDTO> getMilestoneById(@PathVariable("id") UUID id) {
        MilestoneResponseDTO milestone = milestoneService.getMilestoneById(id);
        return ResponseEntity.ok(milestone);
    }

    @PostMapping
    public ResponseEntity<MilestoneResponseDTO> createMilestone(@RequestBody MilestoneRequestDTO requestDTO) {
        MilestoneResponseDTO milestone = milestoneService.createMilestone(requestDTO);
        URI location = URI.create(String.format(API_VERSION + "/milestones/%s", milestone.getId()));
        return ResponseEntity.status(HttpStatus.CREATED).location(location).body(milestone);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<MilestoneResponseDTO> updateMilestone(
            @PathVariable("id") UUID id,
            @RequestBody MilestoneRequestDTO requestDTO
    ) {
        MilestoneResponseDTO updatedMilestone = milestoneService.updateMilestone(id, requestDTO);
        return ResponseEntity.ok(updatedMilestone);
    }

@DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMilestone(@PathVariable("id") UUID id) {
        milestoneService.deleteMilestone(id);
        return ResponseEntity.noContent().build();
    }
}
