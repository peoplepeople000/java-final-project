package com.example.taskmanager.service;

import com.example.taskmanager.exception.BadRequestException;
import com.example.taskmanager.exception.NotFoundException;
import com.example.taskmanager.model.entity.Project;
import com.example.taskmanager.model.entity.ProjectMember;
import com.example.taskmanager.model.entity.User;
import com.example.taskmanager.service.ChangeEventService;
import com.example.taskmanager.service.ChangeEventTypes;
import com.example.taskmanager.repository.TaskRepository;
import com.example.taskmanager.repository.ProjectMemberRepository;
import com.example.taskmanager.repository.ProjectRepository;
import com.example.taskmanager.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

    private static final String ROLE_OWNER = "OWNER";
    private static final String ROLE_MEMBER = "MEMBER";

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TaskRepository taskRepository;
    private final ChangeEventService changeEventService;

    public ProjectService(UserRepository userRepository,
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            TaskRepository taskRepository,
            ChangeEventService changeEventService) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.taskRepository = taskRepository;
        this.changeEventService = changeEventService;
    }

    @Transactional
    public Project createProject(Long currentUserId, String name, String description) {
        User owner = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        Project project = new Project();
        project.setOwner(owner);
        project.setName(name);
        project.setDescription(description);
        Project savedProject = projectRepository.save(project);

        ProjectMember ownerMember = new ProjectMember();
        ownerMember.setProject(savedProject);
        ownerMember.setUser(owner);
        ownerMember.setRole(ROLE_OWNER);
        ownerMember.setJoinedAt(LocalDateTime.now());
        projectMemberRepository.save(ownerMember);

        changeEventService.record(ChangeEventTypes.PROJECT_CREATED, savedProject.getId(), savedProject.getId());
        changeEventService.record(ChangeEventTypes.PROJECT_MEMBERS_UPDATED, savedProject.getId(), savedProject.getId());
        return savedProject;
    }

    @Transactional(readOnly = true)
    public Project getById(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));
    }

    @Transactional(readOnly = true)
    public Project getProjectForUser(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));
        if (project.getOwner() != null && project.getOwner().getId().equals(userId)) {
            return project;
        }
        boolean member = projectMemberRepository.existsByProjectIdAndUserId(projectId, userId);
        if (!member) {
            throw new BadRequestException("Not authorized to view this project");
        }
        return project;
    }

    @Transactional(readOnly = true)
    public List<Project> getProjectsForUser(Long currentUserId) {
        userRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        Map<Long, Project> projects = new LinkedHashMap<>();
        projectRepository.findByOwnerId(currentUserId)
                .forEach(project -> projects.put(project.getId(), project));

        projectMemberRepository.findByUserId(currentUserId).stream()
                .map(ProjectMember::getProject)
                .forEach(project -> projects.put(project.getId(), project));

        return new ArrayList<>(projects.values());
    }

    @Transactional
    public ProjectMember addMember(Long currentUserId, Long projectId, String usernameOrEmail) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));

        ProjectMember currentMembership = projectMemberRepository.findByProjectIdAndUserId(projectId, currentUserId)
                .orElseThrow(() -> new BadRequestException("Only project owner can add members"));

        if (!ROLE_OWNER.equalsIgnoreCase(currentMembership.getRole())) {
            throw new BadRequestException("Only project owner can add members");
        }

        User targetUser = findUserByUsernameOrEmailOrId(usernameOrEmail)
                .orElseThrow(() -> new NotFoundException("Target user not found"));

        projectMemberRepository.findByProjectIdAndUserId(projectId, targetUser.getId())
                .ifPresent(existing -> {
                    throw new BadRequestException("User is already a member of this project");
                });

        ProjectMember newMember = new ProjectMember();
        newMember.setProject(project);
        newMember.setUser(targetUser);
        newMember.setRole(ROLE_MEMBER);
        newMember.setJoinedAt(LocalDateTime.now());
        ProjectMember saved = projectMemberRepository.save(newMember);
        changeEventService.record(ChangeEventTypes.PROJECT_MEMBERS_UPDATED, project.getId(), project.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<ProjectMember> listMembers(Long currentUserId, Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));
        projectMemberRepository.findByProjectIdAndUserId(projectId, currentUserId)
                .orElseThrow(() -> new BadRequestException("User is not a member of this project"));
        return projectMemberRepository.findByProjectId(project.getId());
    }

    private Optional<User> findUserByUsernameOrEmailOrId(String usernameOrEmail) {
        if (usernameOrEmail == null) {
            return Optional.empty();
        }
        String lookup = usernameOrEmail.trim();
        if (lookup.isEmpty()) {
            return Optional.empty();
        }

        Optional<User> byUsername = userRepository.findByUsername(lookup);
        if (byUsername.isPresent()) {
            return byUsername;
        }
        Optional<User> byEmail = userRepository.findByEmail(lookup);
        if (byEmail.isPresent()) {
            return byEmail;
        }
        try {
            Long id = Long.valueOf(lookup);
            return userRepository.findById(id);
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    @Transactional
    public void removeMember(Long currentUserId, Long projectId, Long userIdToRemove) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));

        Long ownerId = project.getOwner().getId();
        if (!ownerId.equals(currentUserId)) {
            throw new BadRequestException("Only project owner can remove members");
        }
        if (ownerId.equals(userIdToRemove)) {
            throw new BadRequestException("Cannot remove project owner");
        }

        projectMemberRepository.findByProjectIdAndUserId(projectId, userIdToRemove)
                .orElseThrow(() -> new NotFoundException("User is not a member of this project"));

        // TODO: if tasks assigned to this user in the project, consider unassigning them.
        projectMemberRepository.deleteByProjectIdAndUserId(projectId, userIdToRemove);
        changeEventService.record(ChangeEventTypes.PROJECT_MEMBERS_UPDATED, projectId, projectId);
    }

    @Transactional
    public void deleteProject(Long currentUserId, Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));

        if (!project.getOwner().getId().equals(currentUserId)) {
            throw new BadRequestException("Only project owner can delete project");
        }

        // Delete related entities in order to avoid FK issues: tasks -> members -> project
        taskRepository.deleteByProjectId(projectId);
        projectMemberRepository.deleteByProjectId(projectId);
        projectRepository.delete(project);
        changeEventService.record(ChangeEventTypes.PROJECT_DELETED, projectId, projectId);
    }

    @Transactional
    public Project updateProject(Long currentUserId, Long projectId, String name, String description) {
        if (name == null || name.trim().isEmpty()) {
            throw new BadRequestException("Project name is required");
        }
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));
        if (!project.getOwner().getId().equals(currentUserId)) {
            throw new BadRequestException("Only project owner can update project");
        }
        project.setName(name.trim());
        project.setDescription(description);
        Project updated = projectRepository.save(project);
        changeEventService.record(ChangeEventTypes.PROJECT_UPDATED, updated.getId(), updated.getId());
        return updated;
    }
}
