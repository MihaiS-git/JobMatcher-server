package com.jobmatcher.server.service;

import com.jobmatcher.server.model.ProposalDetailDTO;
import com.jobmatcher.server.model.ProposalRequestDTO;
import com.jobmatcher.server.model.ProposalSummaryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface IProposalService {
    Page<ProposalSummaryDTO> getProposalsByProjectId(UUID projectId, Pageable pageable);
    ProposalDetailDTO getProposalById(UUID id);
    ProposalDetailDTO createProposal(ProposalRequestDTO requestDTO);
    ProposalDetailDTO updateProposalById(UUID id, ProposalRequestDTO requestDTO);
    void deleteProposalById(UUID id);
}
