package com.jobmatcher.server.mapper;

import com.jobmatcher.server.domain.JobSubcategory;
import com.jobmatcher.server.model.JobSubcategoryDTO;
import org.springframework.stereotype.Component;

@Component
public class JobSubcategoryMapper {
    public JobSubcategoryDTO toDto(JobSubcategory entity){
        if(entity == null) return null;

        return JobSubcategoryDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .build();
    }

    public JobSubcategory toEntity(JobSubcategoryDTO dto) {
        if (dto == null) return null;

        JobSubcategory subcategory = new JobSubcategory();
        subcategory.setId(dto.getId());
        subcategory.setName(dto.getName());
        subcategory.setDescription(dto.getDescription());

        return subcategory;
    }

}
