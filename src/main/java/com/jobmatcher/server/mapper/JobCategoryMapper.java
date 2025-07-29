package com.jobmatcher.server.mapper;

import com.jobmatcher.server.domain.JobCategory;
import com.jobmatcher.server.model.JobCategoryDTO;
import com.jobmatcher.server.model.JobSubcategoryDTO;
import org.springframework.stereotype.Component;

import java.util.List;

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
}
