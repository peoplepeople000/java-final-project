package com.example.taskmanager.model.dto.change;

import com.example.taskmanager.model.entity.ChangeEvent;

public class ChangeEventDto {

    private Long id;
    private String type;
    private Long entityId;
    private Long projectId;
    private Long createdAt;

    public static ChangeEventDto from(ChangeEvent event) {
        ChangeEventDto dto = new ChangeEventDto();
        dto.setId(event.getId());
        dto.setType(event.getType());
        dto.setEntityId(event.getEntityId());
        dto.setProjectId(event.getProjectId());
        dto.setCreatedAt(event.getCreatedAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }
}
