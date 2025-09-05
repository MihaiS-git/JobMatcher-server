package com.jobmatcher.server.service;

import com.jobmatcher.server.domain.ProjectStatus;
import com.jobmatcher.server.model.PagedResponseDTO;
import com.jobmatcher.server.model.ProjectRequestDTO;
import com.jobmatcher.server.model.ProjectResponseDTO;
import com.jobmatcher.server.model.ProjectSummaryDTO;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface IProjectService {

    PagedResponseDTO<ProjectSummaryDTO> getAllProjects(
            String token,
            Pageable pageable,
            ProjectStatus status,
            Long categoryId,
            Long subcategoryId,
            String searchTerm
    );

    ProjectResponseDTO getProjectById(UUID id);

    ProjectResponseDTO createProject(ProjectRequestDTO project);
    ProjectResponseDTO updateProject(UUID id, ProjectRequestDTO requestDto);
    void deleteProject(UUID id);

    PagedResponseDTO<ProjectSummaryDTO> getAllJobFeedProjects(
            Pageable pageable,
            List<ProjectStatus> statusesToFetch,
            Long categoryId,
            Long subcategoryId,
            String searchTerm
    );
}
