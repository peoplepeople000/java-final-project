package com.example.taskmanager.model.dto.task;

import javax.validation.constraints.Size;

public class UpdateTaskRequest {

    @Size(max = 150)
    private String title;

    @Size(max = 1000)
    private String description;

    @Size(max = 20)
    private String status;

    @Size(max = 20)
    private String priority;

    private Long assigneeId;

    @Size(max = 50)
    private String dueDate;

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

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }
}
