package com.jobmatcher.server.model;

import lombok.Builder;
import lombok.Getter;

import java.util.Set;
import java.util.UUID;

@Getter
@Builder
public class PortfolioItemDetailDTO {
    private UUID id;
    private String title;
    private String description;
    private JobCategoryDTO category;
    private Set<JobSubcategoryDTO> subcategories;
    private String demoUrl;
    private String sourceUrl;
    private Set<String> imageUrls;
    private String clientName;
    private UUID freelancerProfileId;
}
