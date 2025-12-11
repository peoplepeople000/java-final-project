package com.example.taskmanager.model.dto.project;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class CreateProjectRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 500)
    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
