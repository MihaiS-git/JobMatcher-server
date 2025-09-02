package com.jobmatcher.server.service;

import com.jobmatcher.server.domain.ProjectStatus;
import com.jobmatcher.server.model.ProjectRequestDTO;
import com.jobmatcher.server.model.ProjectResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Set;
import java.util.UUID;

public interface IProjectService {

    Page<ProjectResponseDTO> getAllProjects(
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
}
