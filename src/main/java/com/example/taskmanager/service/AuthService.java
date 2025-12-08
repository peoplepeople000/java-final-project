package com.example.taskmanager.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.taskmanager.exception.BadRequestException;
import com.example.taskmanager.model.entity.User;

@Service
public class AuthService {

    private final UserService userService;

    public AuthService(UserService userService) {
        this.userService = userService;
    }

    @Transactional
    public User register(String username, String email, String rawPassword) {
        validateUniqueness(username, email);
        User user = new User();
        user.setUsername(username.trim());
        user.setEmail(email.trim().toLowerCase());
        user.setPasswordHash(hashPassword(rawPassword));
        user.setStatus("ACTIVE");
        return userService.save(user);
    }

    @Transactional(readOnly = true)
    public User login(String usernameOrEmail, String rawPassword) {
        String identifier = usernameOrEmail.trim();
        Optional<User> userOpt = userService.findByUsername(identifier);
        if (!userOpt.isPresent()) {
            userOpt = userService.findByEmail(identifier.toLowerCase());
        }
        User user = userOpt.orElseThrow(() -> new BadRequestException("Invalid username/email or password"));
        String hashed = hashPassword(rawPassword);
        if (!hashed.equals(user.getPasswordHash())) {
            throw new BadRequestException("Invalid username/email or password");
        }
        return user;
    }

    private void validateUniqueness(String username, String email) {
        if (userService.findByUsername(username).isPresent()) {
            throw new BadRequestException("Username is already taken");
        }
        if (userService.findByEmail(email).isPresent()) {
            throw new BadRequestException("Email is already registered");
        }
    }

    private String hashPassword(String rawPassword) {
        // TODO: replace with BCrypt when security requirements increase
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Unable to hash password", ex);
        }
    }
}
