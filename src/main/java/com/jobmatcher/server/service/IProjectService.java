package com.jobmatcher.server.service;

import com.jobmatcher.server.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface IProjectService {

    Page<ProjectSummaryDTO> getAllProjects(
            String token,
            Pageable pageable,
            ProjectFilterDTO filter
    );

    Page<ProjectSummaryDTO> getAllJobFeedProjects(
            Pageable pageable,
            ProjectFilterDTO filter
    );

    ProjectDetailDTO getProjectById(UUID id);

    ProjectDetailDTO createProject(ProjectRequestDTO project);
    ProjectDetailDTO updateProject(UUID id, ProjectRequestDTO requestDto);
    void deleteProject(UUID id);

    ProjectDetailDTO updateProjectStatus(UUID projectId, ProjectStatusUpdateDTO status);
}
