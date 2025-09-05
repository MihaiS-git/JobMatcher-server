package com.jobmatcher.server.service;

import com.jobmatcher.server.domain.Milestone;
import com.jobmatcher.server.domain.MilestoneStatus;
import com.jobmatcher.server.domain.PaymentStatus;
import com.jobmatcher.server.model.MilestoneRequestDTO;
import com.jobmatcher.server.model.MilestoneResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

public class MilestoneService implements IMilestoneService{
    @Override
    public Page<MilestoneResponseDTO> getMilestonesByProjectId(UUID projectId, Pageable pageable, MilestoneStatus status, PaymentStatus paymentStatus, LocalDate plannedStartDate, LocalDate plannedEndDate, LocalDate actualStartDate, LocalDate actualEndDate) {
        return null;
    }

    @Override
    public Milestone getMilestoneById(UUID id) {
        return null;
    }

    @Override
    public Milestone createMilestone(MilestoneRequestDTO requestDTO) {
        return null;
    }

    @Override
    public Milestone updateMilestone(UUID id, MilestoneRequestDTO requestDTO) {
        return null;
    }

    @Override
    public void deleteMilestone(UUID id) {

    }
}
