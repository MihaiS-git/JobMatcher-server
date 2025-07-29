package com.jobmatcher.server.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class JobSubcategoryDTO {
    private Long id;
    private String name;
    private String description;
}
