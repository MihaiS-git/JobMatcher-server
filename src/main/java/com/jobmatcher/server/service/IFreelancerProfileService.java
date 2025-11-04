package com.jobmatcher.server.service;

import com.jobmatcher.server.model.FreelancerDetailDTO;
import com.jobmatcher.server.model.FreelancerProfileRequestDTO;
import com.jobmatcher.server.model.FreelancerSummaryDTO;

import java.util.Set;
import java.util.UUID;

public interface IFreelancerProfileService {
    FreelancerDetailDTO getFreelancerProfileById(UUID id);
    FreelancerDetailDTO getFreelancerProfileByUserId(UUID userId);
    FreelancerDetailDTO saveFreelancerProfile(FreelancerProfileRequestDTO dto);
    FreelancerDetailDTO updateFreelancerProfile(UUID id, FreelancerProfileRequestDTO dto);

}
