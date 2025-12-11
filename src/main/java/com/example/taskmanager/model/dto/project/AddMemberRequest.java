package com.example.taskmanager.model.dto.project;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class AddMemberRequest {

    @NotBlank
    @Size(max = 100)
    private String usernameOrEmail;

    public String getUsernameOrEmail() {
        return usernameOrEmail;
    }

    public void setUsernameOrEmail(String usernameOrEmail) {
        this.usernameOrEmail = usernameOrEmail;
    }
}
