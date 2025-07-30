package com.jobmatcher.server.model;

import com.jobmatcher.server.domain.ExperienceLevel;
import lombok.Builder;
import lombok.Getter;

import java.util.Set;
import java.util.UUID;

@Getter
@Builder
public class FreelancerSummaryDTO {

    private UUID userId;
    private UUID profileId;
    private String username;
    private ExperienceLevel experienceLevel;
    private String headline;
    private Set<JobSubcategoryDTO> jobSubcategories;
    private Double hourlyRate;
    private Boolean availableForHire;
    private String pictureUrl;
    private Set<SkillDTO> skills;
    private Set<LanguageDTO> languages;
    private Double rating;

}
