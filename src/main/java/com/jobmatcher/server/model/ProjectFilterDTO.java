package com.jobmatcher.server.model;

import com.jobmatcher.server.domain.ProjectStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ProjectFilterDTO {
    ProjectStatus status;
    Long categoryId;
    Long subcategoryId;
    String searchTerm;
}
