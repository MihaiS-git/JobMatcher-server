package com.jobmatcher.server.controller;

import com.jobmatcher.server.model.ContractDetailDTO;
import com.jobmatcher.server.model.ContractFilterDTO;
import com.jobmatcher.server.model.ContractSummaryDTO;
import com.jobmatcher.server.service.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.UUID;

import static com.jobmatcher.server.model.ApiConstants.API_VERSION;

@RestController
@RequestMapping(API_VERSION + "/contracts")
public class ContractController {
    private final IContractService contractService;

    public ContractController(IContractService contractService) {
        this.contractService = contractService;
    }

    @GetMapping
    public ResponseEntity<Page<ContractSummaryDTO>> getAllContractsByProfileId(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String profileId,
            @ModelAttribute ContractFilterDTO filter,
            Pageable pageable
    ) {
        Page<ContractSummaryDTO> page = contractService.getAllContractsByProfileId(authHeader, profileId, pageable, filter);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContractDetailDTO> getContractById(
            @PathVariable String id
    ) {
        ContractDetailDTO contract = contractService.getContractById(UUID.fromString(id));
        return ResponseEntity.ok(contract);
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<ContractDetailDTO> getContractByProjectId(
            @PathVariable String projectId
    ) {
        ContractDetailDTO contract = contractService.getContractByProjectId(UUID.fromString(projectId));
        return ResponseEntity.ok(contract);
    }

    @DeleteMapping("/{contractId}")
    public ResponseEntity<Void> deleteContractById(
            @PathVariable String contractId
    ) {
        contractService.deleteContractById(UUID.fromString(contractId));
        return ResponseEntity.noContent().build();
    }
}
