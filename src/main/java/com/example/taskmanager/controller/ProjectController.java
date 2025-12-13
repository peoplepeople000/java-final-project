package com.example.taskmanager.controller;

import com.example.taskmanager.exception.BadRequestException;
import com.example.taskmanager.model.dto.project.AddMemberRequest;
import com.example.taskmanager.model.dto.project.CreateProjectRequest;
import com.example.taskmanager.model.dto.project.ProjectMemberResponse;
import com.example.taskmanager.model.dto.project.ProjectResponse;
import com.example.taskmanager.model.entity.Project;
import com.example.taskmanager.model.entity.ProjectMember;
import com.example.taskmanager.service.ProjectService;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(
            @RequestHeader(value = "X-USER-ID", required = false) String userIdHeader,
            @Valid @RequestBody CreateProjectRequest request) {
        Long currentUserId = parseUserId(userIdHeader);
        Project project = projectService.createProject(currentUserId, request.getName(), request.getDescription());
        return ResponseEntity.status(HttpStatus.CREATED).body(ProjectResponse.from(project));
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> listProjects(
            @RequestHeader(value = "X-USER-ID", required = false) String userIdHeader) {
        Long currentUserId = parseUserId(userIdHeader);
        List<ProjectResponse> responses = projectService.getProjectsForUser(currentUserId).stream()
                .map(ProjectResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{projectId}/members")
    public ResponseEntity<ProjectMemberResponse> addMember(
            @RequestHeader(value = "X-USER-ID", required = false) String userIdHeader,
            @PathVariable Long projectId,
            @Valid @RequestBody AddMemberRequest request) {
        Long currentUserId = parseUserId(userIdHeader);
        ProjectMember projectMember = projectService.addMember(currentUserId, projectId, request.getUsernameOrEmail());
        return ResponseEntity.ok(ProjectMemberResponse.from(projectMember));
    }

    @GetMapping("/{projectId}/members")
    public ResponseEntity<List<ProjectMemberResponse>> listMembers(
            @RequestHeader(value = "X-USER-ID", required = false) String userIdHeader,
            @PathVariable Long projectId) {
        Long currentUserId = parseUserId(userIdHeader);
        List<ProjectMemberResponse> responses = projectService.listMembers(currentUserId, projectId).stream()
                .map(ProjectMemberResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/{projectId}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @RequestHeader(value = "X-USER-ID", required = false) String userIdHeader,
            @PathVariable Long projectId,
            @PathVariable Long userId) {
        Long currentUserId = parseUserId(userIdHeader);
        projectService.removeMember(currentUserId, projectId, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(
            @RequestHeader(value = "X-USER-ID", required = false) String userIdHeader,
            @PathVariable Long projectId) {
        Long currentUserId = parseUserId(userIdHeader);
        projectService.deleteProject(currentUserId, projectId);
        return ResponseEntity.noContent().build();
    }

    private Long parseUserId(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.trim().isEmpty()) {
            throw new BadRequestException("X-USER-ID header is required");
        }
        try {
            return Long.valueOf(userIdHeader.trim());
        } catch (NumberFormatException ex) {
            throw new BadRequestException("X-USER-ID header must be a valid number");
        }
    }
}
