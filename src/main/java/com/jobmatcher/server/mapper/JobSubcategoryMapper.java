package com.jobmatcher.server.mapper;

import com.jobmatcher.server.domain.JobSubcategory;
import com.jobmatcher.server.model.JobSubcategoryDTO;
import org.springframework.stereotype.Component;

@Component
public class JobSubcategoryMapper {
    public JobSubcategoryDTO toDto(JobSubcategory subcategory){
        if(subcategory == null) return null;

        return JobSubcategoryDTO.builder()
                .id(subcategory.getId())
                .name(subcategory.getName())
                .description(subcategory.getDescription())
                .build();
    }
}
