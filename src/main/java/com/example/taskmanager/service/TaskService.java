package com.example.taskmanager.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.taskmanager.exception.NotFoundException;
import com.example.taskmanager.model.entity.Project;
import com.example.taskmanager.model.entity.Project;
import com.example.taskmanager.model.entity.Task;
import com.example.taskmanager.model.entity.User;
import com.example.taskmanager.repository.TaskRepository;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectService projectService;
    private final UserService userService;

    public TaskService(TaskRepository taskRepository, ProjectService projectService, UserService userService) {
        this.taskRepository = taskRepository;
        this.projectService = projectService;
        this.userService = userService;
    }

    @Transactional
    public Task createTask(Long projectId, TaskPayload payload) {
        Project project = projectService.getById(projectId);
        Task task = new Task();
        task.setProject(project);
        task.setTitle(payload.getTitle());
        task.setDescription(payload.getDescription());
        if (payload.getStatus() != null) {
            task.setStatus(payload.getStatus());
        }
        if (payload.getPriority() != null) {
            task.setPriority(payload.getPriority());
        }
        if (payload.getAssigneeId() != null) {
            User assignee = userService.getById(payload.getAssigneeId());
            task.setAssignee(assignee);
        }
        task.setDueDate(payload.getDueDate());
        return taskRepository.save(task);
    }

    @Transactional(readOnly = true)
    public List<Task> findByProject(Long projectId) {
        projectService.getById(projectId);
        return taskRepository.findByProjectId(projectId);
    }

    @Transactional
    public Task updateStatus(Long taskId, String status) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
        task.setStatus(status);
        if (task.getDueDate() != null) {
            LocalDateTime now = LocalDateTime.now();
            task.setOverdue(task.getDueDate().isBefore(now));
            task.setDueSoon(!task.isOverdue() && now.plusDays(2).isAfter(task.getDueDate()));
        }
        return taskRepository.save(task);
    }

    public static class TaskPayload {
        private String title;
        private String description;
        private String status;
        private String priority;
        private Long assigneeId;
        private LocalDateTime dueDate;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getPriority() {
            return priority;
        }

        public void setPriority(String priority) {
            this.priority = priority;
        }

        public Long getAssigneeId() {
            return assigneeId;
        }

        public void setAssigneeId(Long assigneeId) {
            this.assigneeId = assigneeId;
        }

        public LocalDateTime getDueDate() {
            return dueDate;
        }

        public void setDueDate(LocalDateTime dueDate) {
            this.dueDate = dueDate;
        }
    }
}
