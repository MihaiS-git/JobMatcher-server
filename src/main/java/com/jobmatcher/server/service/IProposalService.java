package com.jobmatcher.server.service;

import com.jobmatcher.server.domain.ProposalStatus;
import com.jobmatcher.server.model.ProposalDetailDTO;
import com.jobmatcher.server.model.ProposalRequestDTO;
import com.jobmatcher.server.model.ProposalStatusRequestDTO;
import com.jobmatcher.server.model.ProposalSummaryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface IProposalService {
    Page<ProposalSummaryDTO> getProposalsByProjectId(UUID projectId, Pageable pageable, ProposalStatus status);
    Page<ProposalSummaryDTO> getProposalsByFreelancerId(UUID freelancerId, Pageable pageable, ProposalStatus status);
    ProposalDetailDTO getProposalById(UUID id);
    ProposalDetailDTO getProposalByFreelancerIdAndProjectId(UUID freelancerId, UUID projectId);
    ProposalSummaryDTO createProposal(ProposalRequestDTO requestDTO);
    ProposalDetailDTO updateProposalById(UUID id, ProposalRequestDTO requestDTO);

    ProposalDetailDTO updateProposalStatusById(UUID id, ProposalStatusRequestDTO requestDTO);

    void deleteProposalById(UUID id);


}
