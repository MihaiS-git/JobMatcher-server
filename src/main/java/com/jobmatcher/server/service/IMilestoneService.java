package com.jobmatcher.server.service;

import com.jobmatcher.server.model.MilestoneRequestDTO;
import com.jobmatcher.server.model.MilestoneResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface IMilestoneService {
    Page<MilestoneResponseDTO> getMilestonesByContractId(UUID contractId, Pageable pageable);
    MilestoneResponseDTO getMilestoneById(UUID id);
    MilestoneResponseDTO createMilestone(MilestoneRequestDTO requestDTO);
    MilestoneResponseDTO updateMilestone(UUID id, MilestoneRequestDTO requestDTO);
    void deleteMilestone(UUID id);

}
