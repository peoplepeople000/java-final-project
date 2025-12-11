package com.example.taskmanager.model.dto.project;

import com.example.taskmanager.model.entity.ProjectMember;
import java.time.LocalDateTime;

public class ProjectMemberResponse {

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
