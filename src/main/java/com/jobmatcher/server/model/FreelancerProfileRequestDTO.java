package com.jobmatcher.server.model;

import com.jobmatcher.server.domain.ExperienceLevel;
import lombok.Builder;
import lombok.Getter;

import java.util.Set;
import java.util.UUID;

@Getter
@Builder
public class FreelancerProfileRequestDTO {

    private UUID userId;
    private String username;
    private ExperienceLevel experienceLevel;
    private String headline;
    private Set<Long> jobSubcategoryIds;
    private Double hourlyRate;
    private Boolean availableForHire;

    private Set<String> skills;
    private Set<Integer> languageIds;
    private String about;
    private Set<String> socialMedia;
    private String websiteUrl;

}
