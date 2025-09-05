package com.jobmatcher.server.controller;

import com.jobmatcher.server.model.ProposalDetailDTO;
import com.jobmatcher.server.model.ProposalRequestDTO;
import com.jobmatcher.server.model.ProposalSummaryDTO;
import com.jobmatcher.server.service.IProposalService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

import static com.jobmatcher.server.model.ApiConstants.API_VERSION;

@RestController
@RequestMapping(path=API_VERSION + "/proposals")
public class ProposalController {

    private  final IProposalService proposalService;

    public ProposalController(IProposalService proposalService) {
        this.proposalService = proposalService;
    }

    @GetMapping
    public ResponseEntity<Page<ProposalSummaryDTO>> getProposalsByProjectId(
            @RequestParam("projectId") UUID projectId,
            Pageable pageable) {
        Page<ProposalSummaryDTO> proposals = proposalService.getProposalsByProjectId(projectId, pageable);
        return ResponseEntity.ok(proposals);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProposalDetailDTO> getProposalById(@PathVariable("id") UUID id) {
        ProposalDetailDTO proposal = proposalService.getProposalById(id);
        return ResponseEntity.ok(proposal);
    }

    @PostMapping
    public ResponseEntity<ProposalDetailDTO> createProposal(@RequestBody ProposalRequestDTO requestDTO) {
        ProposalDetailDTO createdProposal = proposalService.createProposal(requestDTO);
        URI location = URI.create(String.format(API_VERSION + "/proposals/%s", createdProposal.getId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProposal);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ProposalDetailDTO> updateProposalById(@PathVariable("id") UUID id, @RequestBody ProposalRequestDTO requestDTO) {
        ProposalDetailDTO updatedProposal = proposalService.updateProposalById(id, requestDTO);
        return ResponseEntity.ok(updatedProposal);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProposalById(@PathVariable("id") UUID id) {
        proposalService.deleteProposalById(id);
        return ResponseEntity.noContent().build();
    }
}
