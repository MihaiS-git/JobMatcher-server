package com.jobmatcher.server.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jobmatcher.server.domain.ExperienceLevel;
import com.jobmatcher.server.validator.ValidSkills;
import com.jobmatcher.server.validator.ValidWebsiteUrl;
import com.jobmatcher.server.validator.ValidWebsiteUrlCollection;
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

    @Size(min=2, max = 20, message = "Username must be 2-20 characters")
    @Pattern(regexp = "^[A-Za-zÀ-ÖØ-öø-ÿ0-9._'-]{2,}$", message = "Username contains invalid characters")
    private String username;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private ExperienceLevel experienceLevel;

    @Size(max = 50, message = "Headline must be up to 50 characters")
    @Pattern(regexp = "^$|^[a-zA-Z0-9 _.,!?'\"-]+$", message = "Headline contains invalid characters")
    private String headline;

    private Set<Long> jobSubcategoryIds;

    @DecimalMin(value = "0.0", message = "Hourly rate must be positive")
    @DecimalMax(value = "1000.0", message = "Hourly rate must be realistic")
    private Double hourlyRate;

    private Boolean availableForHire;

    @ValidSkills
    private Set<String> skills;

    private Set<Integer> languageIds;

    @Size(max = 1000, message = "About section must be up to 1000 characters")
    @Pattern(regexp = "^$|^[\\p{L}\\p{N}\\p{P}\\p{Zs}\\n\\r]+$", message = "About section contains invalid characters.")
    private String about;

    @ValidWebsiteUrlCollection
    private Set<String> socialMedia;

    @ValidWebsiteUrl
    private String websiteUrl;

}
