package com.jobmatcher.server.controller;

import com.jobmatcher.server.domain.ProjectStatus;
import com.jobmatcher.server.model.ProjectRequestDTO;
import com.jobmatcher.server.model.ProjectResponseDTO;
import com.jobmatcher.server.service.IProjectService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.jobmatcher.server.model.ApiConstants.API_VERSION;

@RestController
@RequestMapping(path = API_VERSION + "/projects")
public class ProjectController {

    private final IProjectService projectService;

    public ProjectController(IProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public ResponseEntity<Page<ProjectResponseDTO>> getAllProjects(
            @RequestHeader("Authorization") String authHeader,
            Pageable pageable,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) UUID freelancerId,
            @RequestParam(required = false) Set<String> statuses,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) Set<UUID> subcategoryIds,
            @RequestParam(required = false) String searchTerm
    ) {
        String token = authHeader.replace("Bearer ", "").trim();
        return ResponseEntity.ok(
                projectService.getAllProjects(
                        token,
                        pageable,
                        customerId,
                        freelancerId,
                        statuses == null
                                ? null
                                : statuses.stream()
                                .map(status -> ProjectStatus.valueOf(status.toUpperCase()))
                                .collect(Collectors.toSet()),
                        categoryId,
                        subcategoryIds,
                        searchTerm
                ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponseDTO> getProjectById(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String id
    ) {
        String token = authHeader.replace("Bearer ", "").trim();
        ProjectResponseDTO project = projectService.getProjectById(UUID.fromString(id));
        return ResponseEntity.ok(project);
    }

    @PostMapping
    public ResponseEntity<ProjectResponseDTO> createProject(@RequestBody @Valid ProjectRequestDTO requestDto) {
        System.out.println("Creating project with request: " + requestDto.toString());
        ProjectResponseDTO created = projectService.createProject(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponseDTO> updateProject(
            @PathVariable String id,
            @RequestBody @Valid ProjectRequestDTO requestDto
    ) {
        ProjectResponseDTO updated = projectService.updateProject(UUID.fromString(id), requestDto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable String id) {
        projectService.deleteProject(UUID.fromString(id));
        return ResponseEntity.noContent().build();
    }
}
