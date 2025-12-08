package com.example.taskmanager.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.taskmanager.exception.BadRequestException;
import com.example.taskmanager.exception.NotFoundException;
import com.example.taskmanager.model.entity.Project;
import com.example.taskmanager.model.entity.ProjectMember;
import com.example.taskmanager.model.entity.User;
import com.example.taskmanager.repository.ProjectMemberRepository;
import com.example.taskmanager.repository.ProjectRepository;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserService userService;

    public ProjectService(ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            UserService userService) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.userService = userService;
    }

    @Transactional
    public Project createProject(Long ownerId, String name, String description) {
        User owner = userService.getById(ownerId);
        Project project = new Project();
        project.setOwner(owner);
        project.setName(name);
        project.setDescription(description);
        return projectRepository.save(project);
    }

    @Transactional(readOnly = true)
    public Project getById(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));
    }

    @Transactional(readOnly = true)
    public List<Project> findProjectsForUser(Long userId) {
        User user = userService.getById(userId);
        Map<Long, Project> results = new LinkedHashMap<>();
        projectRepository.findByOwnerId(user.getId()).forEach(p -> results.put(p.getId(), p));
        projectMemberRepository.findByUserId(user.getId()).stream()
                .map(ProjectMember::getProject)
                .forEach(p -> results.put(p.getId(), p));
        return new ArrayList<>(results.values());
    }

    @Transactional
    public ProjectMember addMember(Long projectId, Long userId, String role) {
        Project project = getById(projectId);
        User user = userService.getById(userId);
        boolean alreadyMember = projectMemberRepository.findByProjectId(projectId).stream()
                .anyMatch(member -> member.getUser().getId().equals(userId));
        if (project.getOwner().getId().equals(userId) || alreadyMember) {
            throw new BadRequestException("User already belongs to this project");
        }
        ProjectMember member = new ProjectMember();
        member.setProject(project);
        member.setUser(user);
        member.setRole(role == null || role.trim().isEmpty() ? "MEMBER" : role.trim());
        member.setJoinedAt(LocalDateTime.now());
        return projectMemberRepository.save(member);
    }

    @Transactional(readOnly = true)
    public List<ProjectMember> listMembers(Long projectId) {
        getById(projectId); // validate exists
        return projectMemberRepository.findByProjectId(projectId);
    }
}
