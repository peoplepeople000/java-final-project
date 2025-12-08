package com.example.taskmanager.desktop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

public class DesktopApiClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DesktopApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.restTemplate = new RestTemplate();
    }

    public AuthResponse register(String username, String email, String password) {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setEmail(email);
        request.setPassword(password);
        return post("/api/auth/register", request, AuthResponse.class);
    }

    public AuthResponse login(String usernameOrEmail, String password) {
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail(usernameOrEmail);
        request.setPassword(password);
        return post("/api/auth/login", request, AuthResponse.class);
    }

    private <T> T post(String path, Object payload, Class<T> responseType) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> entity = new HttpEntity<>(payload, headers);
            return restTemplate.postForObject(baseUrl + path, entity, responseType);
        } catch (RestClientResponseException ex) {
            String detail = resolveErrorMessage(ex);
            throw new ApiException(ex.getRawStatusCode(), detail, ex);
        } catch (RestClientException ex) {
            throw new IllegalStateException("Failed to reach server: " + ex.getMessage(), ex);
        }
    }

    private String resolveErrorMessage(RestClientResponseException ex) {
        String body = ex.getResponseBodyAsString();
        if (body != null && !body.trim().isEmpty()) {
            try {
                JsonNode node = objectMapper.readTree(body);
                if (node.hasNonNull("message")) {
                    return node.get("message").asText();
                }
                if (node.hasNonNull("error")) {
                    return node.get("error").asText();
                }
            } catch (Exception parsingIgnored) {
                // swallow and fall through to plain text
            }
            return body;
        }
        return ex.getStatusText();
    }

    public static class RegisterRequest {
        private String username;
        private String email;
        private String password;

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

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class LoginRequest {
        private String usernameOrEmail;
        private String password;

        public String getUsernameOrEmail() {
            return usernameOrEmail;
        }

        public void setUsernameOrEmail(String usernameOrEmail) {
            this.usernameOrEmail = usernameOrEmail;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class AuthResponse {
        private Long id;
        private String username;
        private String email;
        private String status;

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

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    public static class ApiException extends RuntimeException {
        private final int status;

        public ApiException(int status, String message, Throwable cause) {
            super(message, cause);
            this.status = status;
        }

        public int getStatus() {
            return status;
        }
    }
}
