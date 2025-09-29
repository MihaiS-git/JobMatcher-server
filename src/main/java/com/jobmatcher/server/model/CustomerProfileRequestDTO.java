package com.jobmatcher.server.model;

import com.jobmatcher.server.validator.ValidWebsiteUrl;
import com.jobmatcher.server.validator.ValidWebsiteUrlCollection;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

import java.util.Set;
import java.util.UUID;

@Getter
@Builder
public class CustomerProfileRequestDTO {

    @NotNull(message = "User ID must be provided")
    private UUID userId;

    @Size(min=2, max = 20, message = "Username must be 2-20 characters")
    private String username;

    @Size(max = 50, message = "Company must be up to 50 characters")
    private String company;

    @Size(max = 1000, message = "About section must be up to 1000 characters")
    private String about;

    private Set<Integer> languageIds;

    @ValidWebsiteUrl
    private String websiteUrl;

    @ValidWebsiteUrlCollection
    private Set<String> socialMedia;

}
