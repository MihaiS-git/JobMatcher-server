package com.jobmatcher.server.controller;

import com.jobmatcher.server.domain.ProjectStatus;
import com.jobmatcher.server.model.*;
import com.jobmatcher.server.service.IProjectService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.jobmatcher.server.model.ApiConstants.API_VERSION;

@RestController
@RequestMapping(path = API_VERSION + "/projects")
public class ProjectController {

    private final IProjectService projectService;

    public ProjectController(IProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public ResponseEntity<Page<ProjectSummaryDTO>> getAllProjects(
            @RequestHeader("Authorization") String authHeader,
            @ParameterObject Pageable pageable,
            @ParameterObject ProjectFilterDTO filter
    ) {
        String token = authHeader.replace("Bearer ", "").trim();
        String status = filter.getStatus() != null ? filter.getStatus() : null;



        String projectStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                projectStatus = status.toUpperCase();
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid project status: " + status);
            }
        }

        filter.setStatus(projectStatus);

        Page<ProjectSummaryDTO> response = projectService.getAllProjects(
                token,
                pageable,
                filter
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/job-feed")
    public ResponseEntity<Page<ProjectSummaryDTO>> getAllJobFeedProjects(
            @ParameterObject Pageable pageable,
            @ParameterObject ProjectFilterDTO filter
    ) {
        Page<ProjectSummaryDTO> projects = projectService.getAllJobFeedProjects(pageable, filter);
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectDetailDTO> getProjectById(
            @PathVariable String id
    ) {
        ProjectDetailDTO project = projectService.getProjectById(UUID.fromString(id));
        return ResponseEntity.ok(project);
    }

    @PostMapping
    public ResponseEntity<ProjectDetailDTO> createProject(@RequestBody @Valid ProjectRequestDTO requestDto) {
        ProjectDetailDTO created = projectService.createProject(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ProjectDetailDTO> updateProject(
            @PathVariable String id,
            @RequestBody @Valid ProjectRequestDTO requestDto
    ) {
        ProjectDetailDTO updated = projectService.updateProject(UUID.fromString(id), requestDto);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/status/{id}")
    public ResponseEntity<ProjectDetailDTO> updateProjectStatus(
            @PathVariable String id,
            @RequestBody @Valid ProjectStatusUpdateDTO requestDto
    ) {
        ProjectDetailDTO updated = projectService.updateProjectStatus(UUID.fromString(id), requestDto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable String id) {
        projectService.deleteProject(UUID.fromString(id));
        return ResponseEntity.noContent().build();
    }
}
