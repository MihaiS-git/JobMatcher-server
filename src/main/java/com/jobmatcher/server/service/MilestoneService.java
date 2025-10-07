package com.jobmatcher.server.service;

import com.jobmatcher.server.domain.*;
import com.jobmatcher.server.exception.ResourceNotFoundException;
import com.jobmatcher.server.mapper.MilestoneMapper;
import com.jobmatcher.server.model.ContractRequestDTO;
import com.jobmatcher.server.model.MilestoneRequestDTO;
import com.jobmatcher.server.model.MilestoneResponseDTO;
import com.jobmatcher.server.repository.ContractRepository;
import com.jobmatcher.server.repository.MilestoneRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Transactional(rollbackFor = Exception.class)
@Service
public class MilestoneService implements IMilestoneService {

    private final MilestoneRepository milestoneRepository;
    private final MilestoneMapper milestoneMapper;
    private final IContractService contractService;
    private final ContractRepository contractRepository;

    public MilestoneService(
            MilestoneRepository milestoneRepository,
            MilestoneMapper milestoneMapper,
            IContractService contractService,
            ContractRepository contractRepository
    ) {
        this.milestoneRepository = milestoneRepository;
        this.milestoneMapper = milestoneMapper;
        this.contractService = contractService;
        this.contractRepository = contractRepository;
    }

    @Transactional(readOnly = true)
    @Override
    public Page<MilestoneResponseDTO> getMilestonesByContractId(UUID contractId, Pageable pageable) {
        return milestoneRepository.findByContractId(contractId, pageable).map(milestoneMapper::toDto);
    }

    @Transactional(readOnly = true)
    @Override
    public MilestoneResponseDTO getMilestoneById(UUID id) {
        Milestone milestone = milestoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone not found"));
        return milestoneMapper.toDto(milestone);
    }

    @Override
    public MilestoneResponseDTO createMilestone(MilestoneRequestDTO requestDTO) {
        Contract contract = contractRepository.findById(requestDTO.getContractId())
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found"));
        if (contract.getStatus() == ContractStatus.COMPLETED || contract.getStatus() == ContractStatus.CANCELLED) {
            throw new IllegalStateException("Cannot add milestone to a completed or cancelled contract");
        }
        Milestone milestone = milestoneMapper.toEntity(requestDTO, contract);
        milestone.setContract(contract);
        Milestone savedMilestone = milestoneRepository.save(milestone);
        contract.getMilestones().add(savedMilestone);
        return milestoneMapper.toDto(savedMilestone);
    }

    @Override
    public MilestoneResponseDTO updateMilestone(UUID id, MilestoneRequestDTO requestDTO) {
        Milestone existentMilestone = milestoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone not found"));

        if (requestDTO.getTitle() != null) existentMilestone.setTitle(requestDTO.getTitle());
        if (requestDTO.getDescription() != null) existentMilestone.setDescription(requestDTO.getDescription());
        if (requestDTO.getAmount() != null) existentMilestone.setAmount(requestDTO.getAmount());
        if (requestDTO.getPenaltyAmount() != null) existentMilestone.setPenaltyAmount(requestDTO.getPenaltyAmount());
        if (requestDTO.getBonusAmount() != null) existentMilestone.setBonusAmount(requestDTO.getBonusAmount());
        if (requestDTO.getEstimatedDuration() != null)
            existentMilestone.setEstimatedDuration(requestDTO.getEstimatedDuration());
        if (requestDTO.getStatus() != null) {
            existentMilestone.setStatus(requestDTO.getStatus());
            Contract contract = contractRepository.findById(existentMilestone.getContract().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Contract not found"));
            Set<Milestone> milestones = contract.getMilestones();

            boolean isCompleted = false;
            for (Milestone milestone : milestones) {
                if (milestone.getStatus() == MilestoneStatus.COMPLETED || milestone.getStatus() == MilestoneStatus.CANCELLED || milestone.getStatus() == MilestoneStatus.NONE) {
                    isCompleted = true;
                } else {
                    isCompleted = false;
                    break;
                }
            }
            if (isCompleted) {
                ContractRequestDTO contractRequestDTO = ContractRequestDTO.builder()
                        .status(ContractStatus.COMPLETED)
                        .paymentStatus(existentMilestone.getPaymentStatus() == PaymentStatus.PAID ? PaymentStatus.PAID : PaymentStatus.PROCESSING)
                        .build();
                contractService.updateContractById(contract.getId(), contractRequestDTO);
            }
        }
        if (requestDTO.getPaymentStatus() != null)
            existentMilestone.setPaymentStatus(requestDTO.getPaymentStatus());
        if (requestDTO.getNotes() != null) existentMilestone.setNotes(requestDTO.getNotes());
        if (requestDTO.getPlannedStartDate() != null)
            existentMilestone.setPlannedStartDate(requestDTO.getPlannedStartDate());
        if (requestDTO.getPlannedEndDate() != null)
            existentMilestone.setPlannedEndDate(requestDTO.getPlannedEndDate());
        if (requestDTO.getActualStartDate() != null)
            existentMilestone.setActualStartDate(requestDTO.getActualStartDate());
        if (requestDTO.getActualEndDate() != null)
            existentMilestone.setActualEndDate(requestDTO.getActualEndDate());
        if (requestDTO.getPriority() != null) existentMilestone.setPriority(requestDTO.getPriority());

        Milestone updatedMilestone = milestoneRepository.save(existentMilestone);

        return milestoneMapper.toDto(updatedMilestone);
    }

    @Override
    public void deleteMilestone(UUID id) {
        Milestone existentMilestone = milestoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone not found"));
        milestoneRepository.delete(existentMilestone);
    }
}
