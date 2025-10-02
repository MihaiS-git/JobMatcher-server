package com.jobmatcher.server.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.Set;
import java.util.UUID;

@Getter
@Builder
@ToString
public class CustomerDetailDTO {
    private UUID profileId;
    private UUID userId;
    private String username;
    private String company;
    private Set<LanguageDTO> languages;
    private Double rating;
    private String pictureUrl;

    private String websiteUrl;
    private Set<String> socialMedia;
    private String about;

    private Set<UUID> contractsIds;
}
