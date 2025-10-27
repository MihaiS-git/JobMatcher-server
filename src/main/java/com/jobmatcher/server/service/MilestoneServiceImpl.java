package com.jobmatcher.server.service;

import com.jobmatcher.server.domain.*;
import com.jobmatcher.server.exception.ResourceNotFoundException;
import com.jobmatcher.server.mapper.MilestoneMapper;
import com.jobmatcher.server.model.*;
import com.jobmatcher.server.repository.ContractRepository;
import com.jobmatcher.server.repository.MilestoneRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Slf4j
@Transactional(rollbackFor = Exception.class)
@Service
public class MilestoneServiceImpl implements IMilestoneService {

    private final MilestoneRepository milestoneRepository;
    private final MilestoneMapper milestoneMapper;
    private final IContractService contractService;
    private final ContractRepository contractRepository;

    public MilestoneServiceImpl(
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
    public MilestoneResponseDTO updateMilestoneStatusById(UUID id, MilestoneStatusRequestDTO requestDTO) {
        Milestone existentMilestone = milestoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone not found"));
        Contract contract = contractRepository.findById(existentMilestone.getContract().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found"));

        ContractStatusRequestDTO contractStatusRequestDTO;

        Set<Milestone> milestones = contract.getMilestones();

        boolean isContractCompleted = true;
        Set<MilestoneStatus> completedStatuses = Set.of(
                MilestoneStatus.COMPLETED,
                MilestoneStatus.CANCELLED,
                MilestoneStatus.PAID
        );

        switch (requestDTO.getStatus()) {
            case PENDING: {
                existentMilestone.setStatus(MilestoneStatus.PENDING);
                contractStatusRequestDTO = ContractStatusRequestDTO.builder()
                        .status(ContractStatus.ACTIVE)
                        .build();
                contractService.updateContractStatusById(contract.getId(), contractStatusRequestDTO);
                break;
            }
            case IN_PROGRESS: {
                existentMilestone.setStatus(MilestoneStatus.IN_PROGRESS);
                contractStatusRequestDTO = ContractStatusRequestDTO.builder()
                        .status(ContractStatus.ACTIVE)
                        .build();
                contractService.updateContractStatusById(contract.getId(), contractStatusRequestDTO);
                break;
            }
            case COMPLETED: {
                log.info("Updating milestone {} status to COMPLETED", id);
                existentMilestone.setStatus(MilestoneStatus.COMPLETED);
                for (Milestone milestone : milestones) {
                    if (!completedStatuses.contains(milestone.getStatus())) {
                        isContractCompleted = false;
                        break;
                    }
                }

                log.info("is contract completed? {}", isContractCompleted);

                if (isContractCompleted) {
                    log.info("All milestones completed, updating contract {} status to COMPLETED", contract.getId());
                    contractStatusRequestDTO = ContractStatusRequestDTO.builder()
                            .status(ContractStatus.COMPLETED)
                            .build();
                    contractService.updateContractStatusById(contract.getId(), contractStatusRequestDTO);
                }
                break;
            }
            case PAID: {
                existentMilestone.setStatus(MilestoneStatus.PAID);

                for (Milestone milestone : milestones) {
                    if (!completedStatuses.contains(milestone.getStatus())) {
                        isContractCompleted = false;
                        break;
                    }
                }
                if (isContractCompleted) {
                    contractStatusRequestDTO = ContractStatusRequestDTO.builder()
                            .status(ContractStatus.COMPLETED)
                            .build();
                    contractService.updateContractStatusById(contract.getId(), contractStatusRequestDTO);
                }
                break;
            }
            case CANCELLED: {
                existentMilestone.setStatus(MilestoneStatus.CANCELLED);
                contractStatusRequestDTO = ContractStatusRequestDTO.builder()
                        .status(ContractStatus.ACTIVE)
                        .build();
                contractService.updateContractStatusById(contract.getId(), contractStatusRequestDTO);
                break;
            }
            default:
                throw new IllegalArgumentException("Invalid milestone status");
        }

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
