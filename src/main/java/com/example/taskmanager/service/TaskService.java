package com.example.taskmanager.service;

import com.example.taskmanager.exception.BadRequestException;
import com.example.taskmanager.exception.NotFoundException;
import com.example.taskmanager.model.entity.Project;
import com.example.taskmanager.model.entity.Task;
import com.example.taskmanager.model.entity.User;
import com.example.taskmanager.model.event.TaskEvent;
import com.example.taskmanager.repository.ProjectMemberRepository;
import com.example.taskmanager.repository.ProjectRepository;
import com.example.taskmanager.repository.TaskRepository;
import com.example.taskmanager.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

    private static final String STATUS_TODO = "TODO";
    private static final String STATUS_DOING = "DOING";
    private static final String STATUS_DONE = "DONE";
    private static final Set<String> ALLOWED_STATUSES = new HashSet<>(Arrays.asList(
            STATUS_TODO, STATUS_DOING, STATUS_DONE
    ));

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public TaskService(TaskRepository taskRepository,
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            UserRepository userRepository,
            SimpMessagingTemplate messagingTemplate) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public Task createTask(Long currentUserId, Long projectId, String title, String description, String status,
            String priority, Long assigneeId, LocalDateTime dueDate) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));

        ensureProjectMembership(projectId, currentUserId);

        User assignee = null;
        if (assigneeId != null) {
            assignee = loadAndValidateAssignee(projectId, assigneeId);
        }

        String resolvedStatus = sanitizeStatus(status);

        Task task = new Task();
        task.setProject(project);
        task.setTitle(title);
        task.setDescription(description);
        task.setStatus(resolvedStatus);
        task.setPriority(priority);
        task.setAssignee(assignee);
        task.setDueDate(dueDate);

        Task savedTask = taskRepository.save(task);
        publishTaskEvent("TASK_CREATED", projectId, savedTask.getId(), currentUserId);
        return savedTask;
    }

    @Transactional(readOnly = true)
    public List<Task> getTasksForProject(Long currentUserId, Long projectId, String statusFilter,
            Long assigneeIdFilter) {
        projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));

        ensureProjectMembership(projectId, currentUserId);

        // TODO: apply filtering by statusFilter and assigneeIdFilter
        return taskRepository.findByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public Task getTaskById(Long currentUserId, Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found"));

        ensureProjectMembership(task.getProject().getId(), currentUserId);
        return task;
    }

    @Transactional
    public Task updateTask(Long currentUserId, Long taskId, String title, String description, String status,
            String priority, Long assigneeId, LocalDateTime dueDate) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found"));

        ensureProjectMembership(task.getProject().getId(), currentUserId);

        User assignee = null;
        if (assigneeId != null) {
            assignee = loadAndValidateAssignee(task.getProject().getId(), assigneeId);
        }

        if (title != null) {
            task.setTitle(title);
        }
        if (description != null) {
            task.setDescription(description);
        }
        if (status != null) {
            task.setStatus(sanitizeStatus(status));
        }
        if (priority != null) {
            task.setPriority(priority);
        }
        if (assigneeId != null) {
            task.setAssignee(assignee);
        }
        if (dueDate != null) {
            task.setDueDate(dueDate);
        }

        Task updated = taskRepository.save(task);
        publishTaskEvent("TASK_UPDATED", task.getProject().getId(), task.getId(), currentUserId);
        return updated;
    }

    @Transactional
    public void deleteTask(Long currentUserId, Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
        Long projectId = task.getProject().getId();
        ensureProjectMembership(projectId, currentUserId);
        taskRepository.delete(task);
        publishTaskEvent("TASK_DELETED", projectId, task.getId(), currentUserId);
    }

    private void ensureProjectMembership(Long projectId, Long userId) {
        projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new BadRequestException("User is not a member of this project"));
    }

    private User loadAndValidateAssignee(Long projectId, Long assigneeId) {
        User assignee = userRepository.findById(assigneeId)
                .orElseThrow(() -> new NotFoundException("Assignee user not found"));

        projectMemberRepository.findByProjectIdAndUserId(projectId, assigneeId)
                .orElseThrow(() -> new BadRequestException("Assignee must be a member of the project"));

        return assignee;
    }

    private String sanitizeStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return STATUS_TODO;
        }
        String upper = status.trim().toUpperCase();
        if (!ALLOWED_STATUSES.contains(upper)) {
            throw new BadRequestException("Invalid status. Allowed values: TODO, DOING, DONE");
        }
        return upper;
    }

    private void publishTaskEvent(String type, Long projectId, Long taskId, Long triggeredBy) {
        if (messagingTemplate != null) {
            messagingTemplate.convertAndSend("/topic/tasks", new TaskEvent(type, projectId, taskId, triggeredBy));
        }
    }
}
