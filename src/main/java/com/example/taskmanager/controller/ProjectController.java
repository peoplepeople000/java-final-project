package com.example.taskmanager.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.taskmanager.model.entity.Project;
import com.example.taskmanager.model.entity.ProjectMember;
import com.example.taskmanager.service.ProjectService;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public ProjectResponse createProject(@Valid @RequestBody ProjectRequest request) {
        Project project = projectService.createProject(request.getOwnerId(), request.getName(), request.getDescription());
        return ProjectResponse.from(project);
    }

    @GetMapping("/user/{userId}")
    public List<ProjectResponse> listUserProjects(@PathVariable Long userId) {
        return projectService.findProjectsForUser(userId).stream()
                .map(ProjectResponse::from)
                .collect(Collectors.toList());
    }

    @PostMapping("/{projectId}/members")
    public ProjectMemberResponse addMember(@PathVariable Long projectId, @Valid @RequestBody MemberRequest request) {
        ProjectMember member = projectService.addMember(projectId, request.getUserId(), request.getRole());
        return ProjectMemberResponse.from(member);
    }

    @GetMapping("/{projectId}/members")
    public List<ProjectMemberResponse> listMembers(@PathVariable Long projectId) {
        return projectService.listMembers(projectId).stream()
                .map(ProjectMemberResponse::from)
                .collect(Collectors.toList());
    }

    public static class ProjectRequest {
        @NotNull
        private Long ownerId;

        @NotBlank
        @Size(max = 100)
        private String name;

        @Size(max = 500)
        private String description;

        public Long getOwnerId() {
            return ownerId;
        }

        public void setOwnerId(Long ownerId) {
            this.ownerId = ownerId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    public static class MemberRequest {
        @NotNull
        private Long userId;

        @Size(max = 20)
        private String role;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }

    public static class ProjectResponse {
        private Long id;
        private String name;
        private String description;
        private Long ownerId;
        private String ownerName;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static ProjectResponse from(Project project) {
            ProjectResponse response = new ProjectResponse();
            response.setId(project.getId());
            response.setName(project.getName());
            response.setDescription(project.getDescription());
            response.setOwnerId(project.getOwner().getId());
            response.setOwnerName(project.getOwner().getUsername());
            response.setCreatedAt(project.getCreatedAt());
            response.setUpdatedAt(project.getUpdatedAt());
            return response;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Long getOwnerId() {
            return ownerId;
        }

        public void setOwnerId(Long ownerId) {
            this.ownerId = ownerId;
        }

        public String getOwnerName() {
            return ownerName;
        }

        public void setOwnerName(String ownerName) {
            this.ownerName = ownerName;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public LocalDateTime getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
        }

        @Override
        public String toString() {
            return name + " (#" + id + ")";
        }
    }

    public static class ProjectMemberResponse {
        private Long id;
        private Long projectId;
        private Long userId;
        private String username;
        private String role;
        private LocalDateTime joinedAt;

        public static ProjectMemberResponse from(ProjectMember member) {
            ProjectMemberResponse response = new ProjectMemberResponse();
            response.setId(member.getId());
            response.setProjectId(member.getProject().getId());
            response.setUserId(member.getUser().getId());
            response.setUsername(member.getUser().getUsername());
            response.setRole(member.getRole());
            response.setJoinedAt(member.getJoinedAt());
            return response;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getProjectId() {
            return projectId;
        }

        public void setProjectId(Long projectId) {
            this.projectId = projectId;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public LocalDateTime getJoinedAt() {
            return joinedAt;
        }

        public void setJoinedAt(LocalDateTime joinedAt) {
            this.joinedAt = joinedAt;
        }
    }
}
