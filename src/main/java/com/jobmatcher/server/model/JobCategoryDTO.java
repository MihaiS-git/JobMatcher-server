package com.jobmatcher.server.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class JobCategoryDTO {
    private Long id;
    private String name;
    private String description;
    private List<JobSubcategoryDTO> subcategories;
}
