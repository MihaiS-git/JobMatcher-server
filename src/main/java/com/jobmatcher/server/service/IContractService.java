package com.jobmatcher.server.service;

import com.jobmatcher.server.domain.Contract;
import com.jobmatcher.server.model.ContractDetailDTO;
import com.jobmatcher.server.model.ContractRequestDTO;
import com.jobmatcher.server.model.ContractSummaryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface IContractService {
    Page<ContractSummaryDTO> getAllContractsByCustomerId(UUID customerId, Pageable pageable);
    Page<ContractSummaryDTO> getAllContractsByFreelancerId(UUID freelancerId, Pageable pageable);
    ContractDetailDTO getContractById(UUID contractId);
    ContractDetailDTO getContractByProjectId(UUID projectId);
    ContractDetailDTO createContract(Contract contract);
    ContractDetailDTO updateContract(UUID contractId, ContractRequestDTO requestDTO);
    void deleteContract(UUID contractId);
}
