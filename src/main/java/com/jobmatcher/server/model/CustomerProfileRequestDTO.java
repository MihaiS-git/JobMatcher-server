package com.jobmatcher.server.model;

import lombok.Builder;
import lombok.Getter;

import java.util.Set;
import java.util.UUID;

@Getter
@Builder
public class CustomerProfileRequestDTO {
    private UUID userId;
    private String username;
    private String company;
    private String about;
    private Set<Integer> languageIds;
    private String websiteUrl;
    private Set<String> socialMedia;

}
