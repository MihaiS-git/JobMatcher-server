package com.jobmatcher.server.service;

import com.jobmatcher.server.model.ProposalDetailDTO;
import com.jobmatcher.server.model.ProposalRequestDTO;
import com.jobmatcher.server.model.ProposalSummaryDTO;
import org.springframework.data.domain.Page;

import java.awt.print.Pageable;
import java.math.BigDecimal;
import java.util.UUID;

public interface IProposalService {
    Page<ProposalSummaryDTO> getProposalsByProjectId(
            UUID projectId,
            Pageable pageable,
            BigDecimal amount,
            Integer estimatedDuration);
    ProposalDetailDTO getProposalById(UUID id);
    ProposalDetailDTO createProposal(ProposalRequestDTO requestDTO);
    ProposalDetailDTO updateProposal(UUID id, ProposalRequestDTO requestDTO);
    void deleteProposal(UUID id);
}
