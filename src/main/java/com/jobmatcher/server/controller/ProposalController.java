package com.jobmatcher.server.controller;

import com.jobmatcher.server.domain.ProposalStatus;
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
            Pageable pageable,
            @RequestParam("projectId") String projectId,
            @RequestParam(value = "status", required = false) ProposalStatus status
    ) {
        Page<ProposalSummaryDTO> proposals = proposalService.getProposalsByProjectId(UUID.fromString(projectId),pageable, status);
        return ResponseEntity.ok(proposals);
    }

    @GetMapping(path = "/freelancer/{freelancerId}")
    public ResponseEntity<Page<ProposalSummaryDTO>> getProposalsByFreelancerId(
            @PathVariable("freelancerId") String freelancerId,
            Pageable pageable,
            @RequestParam(value = "status", required = false) ProposalStatus status
    ) {
        Page<ProposalSummaryDTO> proposals = proposalService.getProposalsByFreelancerId(UUID.fromString(freelancerId), pageable, status);
        return ResponseEntity.ok(proposals);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProposalDetailDTO> getProposalById(@PathVariable("id") String id) {
        ProposalDetailDTO proposal = proposalService.getProposalById(UUID.fromString(id));
        return ResponseEntity.ok(proposal);
    }

    @GetMapping("/by-freelancer-and-project")
    public ResponseEntity<ProposalDetailDTO> getProposalByFreelancerAndProject(
            @RequestParam("freelancerId") String freelancerId,
            @RequestParam("projectId") String projectId
    ){
        ProposalDetailDTO proposal = proposalService.getProposalByFreelancerIdAndProjectId(UUID.fromString(freelancerId), UUID.fromString(projectId));
        return ResponseEntity.ok(proposal);
    }

    @PostMapping
    public ResponseEntity<ProposalSummaryDTO> createProposal(@RequestBody ProposalRequestDTO requestDTO) {
        ProposalSummaryDTO createdProposal = proposalService.createProposal(requestDTO);
        URI location = URI.create(String.format(API_VERSION + "/proposals/%s", createdProposal.getId()));
        return ResponseEntity.status(HttpStatus.CREATED).location(location).body(createdProposal);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ProposalDetailDTO> updateProposalById(@PathVariable("id") String id, @RequestBody ProposalRequestDTO requestDTO) {
        ProposalDetailDTO updatedProposal = proposalService.updateProposalById(UUID.fromString(id), requestDTO);
        return ResponseEntity.ok(updatedProposal);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProposalById(@PathVariable("id") String id) {
        proposalService.deleteProposalById(UUID.fromString(id));
        return ResponseEntity.noContent().build();
    }
}
