package com.jobmatcher.server.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jobmatcher.server.domain.ExperienceLevel;
import com.jobmatcher.server.validator.ValidSkills;
import com.jobmatcher.server.validator.ValidWebsiteUrl;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;

import java.util.Set;
import java.util.UUID;

@Getter
@Builder
public class FreelancerProfileRequestDTO {

    @NotNull(message = "User ID must be provided")
    private UUID userId;

    @Size(min = 3, max = 20, message = "Username must be 3–20 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores")
    private String username;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private ExperienceLevel experienceLevel;

    @Size(min = 3, max = 50, message = "Headline must be 3–50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9 _.,!?'\"-]+$", message = "Headline contains invalid characters")
    private String headline;

    private Set<Long> jobSubcategoryIds;

    @DecimalMin(value = "0.0", message = "Hourly rate must be positive")
    @DecimalMax(value = "1000.0", message = "Hourly rate must be realistic")
    private Double hourlyRate;

    private Boolean availableForHire;

    @ValidSkills
    private Set<String> skills;

    private Set<Integer> languageIds;

    @Size(min = 0, max = 1000, message = "About section must be up to 1000 characters")
    @Pattern(regexp = "^[\\p{L}\\p{N}\\p{P}\\p{Zs}\\n\\r]+$", message = "About section contains invalid characters.")
    private String about;

    @ValidWebsiteUrl
    private Set<String> socialMedia;

    @ValidWebsiteUrl
    private String websiteUrl;

}
