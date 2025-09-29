package com.jobmatcher.server.model;

import com.jobmatcher.server.validator.ValidWebsiteUrl;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;

import java.util.Set;
import java.util.UUID;

@Getter
@Builder
public class PortfolioItemRequestDTO {

    @Size(min = 2, max = 100, message = "Title must be between 2 and 100 characters")
    @NotBlank(message = "Title cannot be blank")
    private String title;

    @Size(max = 1000, message = "Description must be up to 1000 characters")
    @NotBlank(message = "Description cannot be blank")
    private String description;

    @NotNull(message = "Category must be provided")
    private Long categoryId;

    @NotEmpty(message = "At least one subcategory must be selected")
    private Set<Long> subcategoryIds;

    @ValidWebsiteUrl
    private String demoUrl;

    @ValidWebsiteUrl
    private String sourceUrl;

    @Size(max = 100, message = "Client name must be up to 100 characters")
    @NotBlank(message = "Client name cannot be blank")
    private String clientName;

    @NotNull(message = "Freelancer profile ID must be provided")
    private UUID freelancerProfileId;
}
