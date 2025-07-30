package com.jobmatcher.server.model;

import lombok.Builder;
import lombok.Getter;

import java.util.Set;
import java.util.UUID;

@Getter
@Builder
public class CustomerSummaryDTO {
    private UUID profileId;
    private UUID userId;
    private String username;
    private String company;
    private Set<LanguageDTO> languages;
    private Double rating;
    private String pictureUrl;
}
