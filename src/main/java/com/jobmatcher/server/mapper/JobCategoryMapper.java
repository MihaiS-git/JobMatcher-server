package com.jobmatcher.server.mapper;

import com.jobmatcher.server.domain.JobCategory;
import com.jobmatcher.server.model.JobCategoryDTO;
import com.jobmatcher.server.model.JobSubcategoryDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class JobCategoryMapper {

    private final JobSubcategoryMapper jobSubcategoryMapper;

    public JobCategoryMapper(JobSubcategoryMapper jobSubcategoryMapper) {
        this.jobSubcategoryMapper = jobSubcategoryMapper;
    }

    public JobCategoryDTO toDto(JobCategory category){
        if(category == null) return null;

        List<JobSubcategoryDTO> subcategoryDTOS = category.getSubcategories().stream()
                .map(jobSubcategoryMapper::toDto)
                .toList();

        return JobCategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .subcategories(subcategoryDTOS)
                .build();
    }

    public JobCategory toEntity(JobCategoryDTO dto) {
        if (dto == null) return null;

        JobCategory category = new JobCategory();
        category.setId(dto.getId());
        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        category.setSubcategories(dto.getSubcategories().stream()
                .map(jobSubcategoryMapper::toEntity)
                .collect(Collectors.toSet()));

        return category;
    }
}
