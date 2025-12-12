package com.example.taskmanager.controller;

import com.example.taskmanager.exception.BadRequestException;
import com.example.taskmanager.model.dto.task.CreateTaskRequest;
import com.example.taskmanager.model.dto.task.TaskResponse;
import com.example.taskmanager.model.dto.task.UpdateTaskRequest;
import com.example.taskmanager.model.entity.Task;
import com.example.taskmanager.service.TaskService;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping("/projects/{projectId}/tasks")
    public ResponseEntity<TaskResponse> createTask(
            @RequestHeader(value = "X-USER-ID", required = false) String userIdHeader,
            @PathVariable Long projectId,
            @Valid @RequestBody CreateTaskRequest request) {
        Long currentUserId = parseUserId(userIdHeader);
        LocalDateTime dueDate = parseDueDate(request.getDueDate());

        Task task = taskService.createTask(
                currentUserId,
                projectId,
                request.getTitle(),
                request.getDescription(),
                request.getStatus(),
                request.getPriority(),
                request.getAssigneeId(),
                dueDate
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(TaskResponse.from(task));
    }

    @GetMapping("/projects/{projectId}/tasks")
    public ResponseEntity<List<TaskResponse>> listTasks(
            @RequestHeader(value = "X-USER-ID", required = false) String userIdHeader,
            @PathVariable Long projectId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "assigneeId", required = false) Long assigneeId) {
        Long currentUserId = parseUserId(userIdHeader);
        List<TaskResponse> responses = taskService.getTasksForProject(currentUserId, projectId, status, assigneeId)
                .stream()
                .map(TaskResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<TaskResponse> getTask(
            @RequestHeader(value = "X-USER-ID", required = false) String userIdHeader,
            @PathVariable Long taskId) {
        Long currentUserId = parseUserId(userIdHeader);
        Task task = taskService.getTaskById(currentUserId, taskId);
        return ResponseEntity.ok(TaskResponse.from(task));
    }

    @PatchMapping("/tasks/{taskId}")
    public ResponseEntity<TaskResponse> updateTask(
            @RequestHeader(value = "X-USER-ID", required = false) String userIdHeader,
            @PathVariable Long taskId,
            @Valid @RequestBody UpdateTaskRequest request) {
        Long currentUserId = parseUserId(userIdHeader);
        LocalDateTime dueDate = parseDueDate(request.getDueDate());

        Task updated = taskService.updateTask(
                currentUserId,
                taskId,
                request.getTitle(),
                request.getDescription(),
                request.getStatus(),
                request.getPriority(),
                request.getAssigneeId(),
                dueDate
        );

        return ResponseEntity.ok(TaskResponse.from(updated));
    }

    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<Void> deleteTask(
            @RequestHeader(value = "X-USER-ID", required = false) String userIdHeader,
            @PathVariable Long taskId) {
        Long currentUserId = parseUserId(userIdHeader);
        taskService.deleteTask(currentUserId, taskId);
        return ResponseEntity.noContent().build();
    }

    private Long parseUserId(String headerValue) {
        if (headerValue == null || headerValue.trim().isEmpty()) {
            throw new BadRequestException("Missing or invalid X-USER-ID header");
        }
        try {
            return Long.valueOf(headerValue.trim());
        } catch (NumberFormatException ex) {
            throw new BadRequestException("Missing or invalid X-USER-ID header");
        }
    }

    private LocalDateTime parseDueDate(String dueDate) {
        if (dueDate == null || dueDate.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dueDate.trim());
        } catch (DateTimeParseException ex) {
            throw new BadRequestException("Invalid dueDate format. Expected ISO-8601 LocalDateTime");
        }
    }
}
