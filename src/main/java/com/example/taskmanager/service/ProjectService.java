package com.example.taskmanager.service;

import com.example.taskmanager.exception.BadRequestException;
import com.example.taskmanager.exception.NotFoundException;
import com.example.taskmanager.model.entity.Project;
import com.example.taskmanager.model.entity.ProjectMember;
import com.example.taskmanager.model.entity.User;
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

    public ProjectService(UserRepository userRepository,
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
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

        return savedProject;
    }

    @Transactional(readOnly = true)
    public Project getById(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));
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

        User targetUser = findUserByUsernameOrEmail(usernameOrEmail)
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
        return projectMemberRepository.save(newMember);
    }

    private Optional<User> findUserByUsernameOrEmail(String usernameOrEmail) {
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
        return userRepository.findByEmail(lookup);
    }
}
