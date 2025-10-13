package com.jobmatcher.server.model;

import com.jobmatcher.server.domain.ProjectStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProjectStatusUpdateDTO {
    @NotNull
    private ProjectStatus status;
}
