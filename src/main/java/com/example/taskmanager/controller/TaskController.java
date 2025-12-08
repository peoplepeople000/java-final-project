package com.example.taskmanager.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.taskmanager.model.entity.Task;
import com.example.taskmanager.service.TaskService;
import com.example.taskmanager.service.TaskService.TaskPayload;

@RestController
@RequestMapping("/api")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping("/projects/{projectId}/tasks")
    public TaskResponse createTask(@PathVariable Long projectId, @Valid @RequestBody TaskRequest request) {
        TaskPayload payload = new TaskPayload();
        payload.setTitle(request.getTitle());
        payload.setDescription(request.getDescription());
        payload.setPriority(request.getPriority());
        payload.setStatus(request.getStatus());
        payload.setAssigneeId(request.getAssigneeId());
        payload.setDueDate(request.getDueDate());
        Task task = taskService.createTask(projectId, payload);
        return TaskResponse.from(task);
    }

    @GetMapping("/projects/{projectId}/tasks")
    public List<TaskResponse> listTasks(@PathVariable Long projectId) {
        return taskService.findByProject(projectId).stream()
                .map(TaskResponse::from)
                .collect(Collectors.toList());
    }

    @PatchMapping("/tasks/{taskId}/status")
    public TaskResponse updateStatus(@PathVariable Long taskId, @Valid @RequestBody TaskStatusRequest request) {
        Task task = taskService.updateStatus(taskId, request.getStatus());
        return TaskResponse.from(task);
    }

    public static class TaskRequest {
        @NotBlank
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

    public static class TaskStatusRequest {
        @NotBlank
        private String status;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    public static class TaskResponse {
        private Long id;
        private Long projectId;
        private String title;
        private String description;
        private String status;
        private String priority;
        private Long assigneeId;
        private String assigneeName;
        private LocalDateTime dueDate;
        private boolean dueSoon;
        private boolean overdue;

        public static TaskResponse from(Task task) {
            TaskResponse response = new TaskResponse();
            response.setId(task.getId());
            response.setProjectId(task.getProject().getId());
            response.setTitle(task.getTitle());
            response.setDescription(task.getDescription());
            response.setStatus(task.getStatus());
            response.setPriority(task.getPriority());
            if (task.getAssignee() != null) {
                response.setAssigneeId(task.getAssignee().getId());
                response.setAssigneeName(task.getAssignee().getUsername());
            }
            response.setDueDate(task.getDueDate());
            response.setDueSoon(task.isDueSoon());
            response.setOverdue(task.isOverdue());
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

        public String getAssigneeName() {
            return assigneeName;
        }

        public void setAssigneeName(String assigneeName) {
            this.assigneeName = assigneeName;
        }

        public LocalDateTime getDueDate() {
            return dueDate;
        }

        public void setDueDate(LocalDateTime dueDate) {
            this.dueDate = dueDate;
        }

        public boolean isDueSoon() {
            return dueSoon;
        }

        public void setDueSoon(boolean dueSoon) {
            this.dueSoon = dueSoon;
        }

        public boolean isOverdue() {
            return overdue;
        }

        public void setOverdue(boolean overdue) {
            this.overdue = overdue;
        }

        @Override
        public String toString() {
            return title + " [" + status + "]";
        }
    }
}
