package com.jobmatcher.server.service;

import com.jobmatcher.server.model.ContractDetailDTO;
import com.jobmatcher.server.model.ContractFilterDTO;
import com.jobmatcher.server.model.ContractRequestDTO;
import com.jobmatcher.server.model.ContractSummaryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface IContractService {
    Page<ContractSummaryDTO> getAllContracts(
            String token,
            Pageable pageable,
            ContractFilterDTO filter
    );
    ContractDetailDTO getContractById(UUID contractId);
    ContractDetailDTO getContractByProjectId(UUID projectId);
    ContractDetailDTO updateContractById(UUID contractId, ContractRequestDTO requestDTO);
    void deleteContractById(UUID contractId);

}
