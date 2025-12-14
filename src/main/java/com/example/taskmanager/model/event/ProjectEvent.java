package com.example.taskmanager.model.event;

public class ProjectEvent {

    private String type;
    private Long projectId;
    private Long triggeredBy;

    public ProjectEvent() {
    }

    public ProjectEvent(String type, Long projectId, Long triggeredBy) {
        this.type = type;
        this.projectId = projectId;
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

    public Long getTriggeredBy() {
        return triggeredBy;
    }

    public void setTriggeredBy(Long triggeredBy) {
        this.triggeredBy = triggeredBy;
    }
}
