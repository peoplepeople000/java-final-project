package com.example.taskmanager.controller;

import com.example.taskmanager.exception.BadRequestException;
import com.example.taskmanager.model.entity.User;
import com.example.taskmanager.repository.UserRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> listUsers(
            @RequestHeader(value = "X-USER-ID", required = false) String userIdHeader) {
        parseUserId(userIdHeader);
        List<UserResponse> users = userRepository.findAll().stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    private Long parseUserId(String header) {
        if (header == null || header.trim().isEmpty()) {
            throw new BadRequestException("X-USER-ID header is required");
        }
        try {
            return Long.valueOf(header.trim());
        } catch (NumberFormatException ex) {
            throw new BadRequestException("X-USER-ID header must be a valid number");
        }
    }

    public static class UserResponse {
        private Long id;
        private String username;
        private String email;

        public static UserResponse from(User user) {
            UserResponse resp = new UserResponse();
            resp.setId(user.getId());
            resp.setUsername(user.getUsername());
            resp.setEmail(user.getEmail());
            return resp;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }
}
