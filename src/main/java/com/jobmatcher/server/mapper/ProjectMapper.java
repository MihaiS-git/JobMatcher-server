package com.jobmatcher.server.mapper;

import com.jobmatcher.server.domain.*;
import com.jobmatcher.server.model.ProjectRequestDTO;
import com.jobmatcher.server.model.ProjectResponseDTO;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ProjectMapper {

    private final CustomerProfileMapper customerProfileMapper;
    private final FreelancerProfileMapper freelancerProfileMapper;
    private final JobCategoryMapper jobCategoryMapper;
    private final JobSubcategoryMapper jobSubcategoryMapper;

    public ProjectMapper(
            CustomerProfileMapper customerProfileMapper,
            FreelancerProfileMapper freelancerProfileMapper,
            JobCategoryMapper jobCategoryMapper,
            JobSubcategoryMapper jobSubcategoryMapper
    ) {
        this.customerProfileMapper = customerProfileMapper;
        this.freelancerProfileMapper = freelancerProfileMapper;
        this.jobCategoryMapper = jobCategoryMapper;
        this.jobSubcategoryMapper = jobSubcategoryMapper;
    }

    public ProjectResponseDTO toDto(Project entity){
        if (entity == null) {
            return null;
        }

        return ProjectResponseDTO.builder()
                .id(entity.getId())
                .customer(entity.getCustomer() != null ? customerProfileMapper.toCustomerSummaryDto(entity.getCustomer(), false) : null)
                .freelancer(entity.getFreelancer() != null ? freelancerProfileMapper.toFreelancerSummaryDto(entity.getFreelancer()) : null)
                .title(entity.getTitle())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .budget(entity.getBudget())
                .paymentType(entity.getPaymentType())
                .deadline(entity.getDeadline())
                .category(entity.getCategory() != null ? jobCategoryMapper.toDto(entity.getCategory()) : null)
                .subcategories(entity.getSubcategories() != null ? entity.getSubcategories().stream()
                        .map(jobSubcategoryMapper::toDto).collect(Collectors.toSet()) : Set.of())
                .build();
    }

    public Project toEntity(
            ProjectRequestDTO dto,
            CustomerProfile customer,
            FreelancerProfile freelancer,
            JobCategory category,
            Set<JobSubcategory> subcategories
    ) {
        if (dto == null) {
            return null;
        }

        Project project = new Project();
        project.setCustomer(customer);
        project.setFreelancer(freelancer);
        project.setTitle(dto.getTitle());
        project.setDescription(dto.getDescription());
        project.setStatus(dto.getStatus());
        project.setBudget(dto.getBudget());
        project.setPaymentType(dto.getPaymentType());
        project.setDeadline(dto.getDeadline());
        project.setCategory(category);
        project.setSubcategories(subcategories);

        return project;
    }
}
