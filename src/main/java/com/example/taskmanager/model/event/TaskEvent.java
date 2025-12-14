package com.example.taskmanager.model.event;

public class TaskEvent {

    private String type;
    private Long projectId;
    private Long taskId;
    private Long triggeredBy;

    public TaskEvent() {
    }

    public TaskEvent(String type, Long projectId, Long taskId, Long triggeredBy) {
        this.type = type;
        this.projectId = projectId;
        this.taskId = taskId;
        this.triggeredBy = triggeredBy;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getTriggeredBy() {
        return triggeredBy;
    }

    public void setTriggeredBy(Long triggeredBy) {
        this.triggeredBy = triggeredBy;
    }
}
