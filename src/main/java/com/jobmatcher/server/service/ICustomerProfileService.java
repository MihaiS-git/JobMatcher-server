package com.jobmatcher.server.service;

import com.jobmatcher.server.model.CustomerDetailDTO;
import com.jobmatcher.server.model.CustomerProfileRequestDTO;
import com.jobmatcher.server.model.CustomerSummaryDTO;
import com.jobmatcher.server.model.FreelancerDetailDTO;

import java.util.Set;
import java.util.UUID;

public interface ICustomerProfileService {
    Set<CustomerSummaryDTO> getAllCustomerProfiles();
    CustomerDetailDTO getCustomerProfileById(UUID id);
    CustomerDetailDTO getCustomerProfileByUserId(UUID userId);
    CustomerDetailDTO saveCustomerProfile(CustomerProfileRequestDTO dto);
    CustomerDetailDTO updateCustomerProfile(UUID id, CustomerProfileRequestDTO dto);

}
