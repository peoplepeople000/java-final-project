package com.example.taskmanager.desktop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

public class DesktopApiClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private AuthResponse currentUser;

    public DesktopApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
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
        AuthResponse response = post("/api/auth/login", request, AuthResponse.class);
        this.currentUser = response;
        return response;
    }

    public AuthResponse getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(AuthResponse currentUser) {
        this.currentUser = currentUser;
    }

    public List<ProjectDto> listProjects() {
        return exchangeWithAuth("/api/projects", HttpMethod.GET, null,
                new ParameterizedTypeReference<List<ProjectDto>>() {});
    }

    public ProjectDto createProject(String name, String description) {
        ProjectCreateRequest request = new ProjectCreateRequest();
        request.setName(name);
        request.setDescription(description);
        return exchangeWithAuth("/api/projects", HttpMethod.POST, request,
                new ParameterizedTypeReference<ProjectDto>() {});
    }

    public List<TaskDto> listTasks(Long projectId) {
        return exchangeWithAuth("/api/projects/" + projectId + "/tasks", HttpMethod.GET, null,
                new ParameterizedTypeReference<List<TaskDto>>() {});
    }

    public TaskDto createTask(Long projectId, String title, String description, String status, String priority,
            Long assigneeId, String dueDate) {
        TaskCreateRequest request = new TaskCreateRequest();
        request.setTitle(title);
        request.setDescription(description);
        request.setStatus(status);
        request.setPriority(priority);
        request.setAssigneeId(assigneeId);
        request.setDueDate(dueDate);
        return exchangeWithAuth("/api/projects/" + projectId + "/tasks", HttpMethod.POST, request,
                new ParameterizedTypeReference<TaskDto>() {});
    }

    public TaskDto updateTaskStatus(Long taskId, String status) {
        TaskUpdateRequest request = new TaskUpdateRequest();
        request.setStatus(status);
        return exchangeWithAuth("/api/tasks/" + taskId, HttpMethod.PATCH, request,
                new ParameterizedTypeReference<TaskDto>() {});
    }

    public void deleteTask(Long taskId) {
        exchangeWithAuth("/api/tasks/" + taskId, HttpMethod.DELETE, null,
                new ParameterizedTypeReference<Void>() {});
    }

    public List<UserDto> listAllUsers() {
        return exchangeWithAuth("/api/users", HttpMethod.GET, null,
                new ParameterizedTypeReference<List<UserDto>>() {});
    }

    public List<MemberDto> listProjectMembers(Long projectId) {
        return exchangeWithAuth("/api/projects/" + projectId + "/members", HttpMethod.GET, null,
                new ParameterizedTypeReference<List<MemberDto>>() {});
    }

    public void addProjectMember(Long projectId, Long userId) {
        AddMemberRequest request = new AddMemberRequest();
        request.setUsernameOrEmail(String.valueOf(userId));
        exchangeWithAuth("/api/projects/" + projectId + "/members", HttpMethod.POST, request,
                new ParameterizedTypeReference<Void>() {});
    }

    public void removeProjectMember(Long projectId, Long userId) {
        exchangeWithAuth("/api/projects/" + projectId + "/members/" + userId, HttpMethod.DELETE, null,
                new ParameterizedTypeReference<Void>() {});
    }

    public void deleteProject(Long projectId) {
        exchangeWithAuth("/api/projects/" + projectId, HttpMethod.DELETE, null,
                new ParameterizedTypeReference<Void>() {});
    }

    public ProjectDto updateProject(Long projectId, String name, String description) {
        ProjectUpdateRequest request = new ProjectUpdateRequest();
        request.setName(name);
        request.setDescription(description);
        return exchangeWithAuth("/api/projects/" + projectId, HttpMethod.PATCH, request,
                new ParameterizedTypeReference<ProjectDto>() {});
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

    private <T> T exchangeWithAuth(String path, HttpMethod method, Object payload,
            ParameterizedTypeReference<T> responseType) {
        if (currentUser == null || currentUser.getId() == null) {
            throw new ApiException(401, "Not logged in", null);
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("X-USER-ID", String.valueOf(currentUser.getId()));
            HttpEntity<Object> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<T> response = restTemplate.exchange(baseUrl + path, method, entity, responseType);
            return response.getBody();
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

    public static class ProjectDto {
        private Long id;
        private String name;
        private String description;
        private Long ownerId;
        private String ownerUsername;
        private String createdAt;
        private String updatedAt;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

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

        public Long getOwnerId() {
            return ownerId;
        }

        public void setOwnerId(Long ownerId) {
            this.ownerId = ownerId;
        }

        public String getOwnerUsername() {
            return ownerUsername;
        }

        public void setOwnerUsername(String ownerUsername) {
            this.ownerUsername = ownerUsername;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }

        public String getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(String updatedAt) {
            this.updatedAt = updatedAt;
        }
    }

    public static class TaskDto {
        private Long id;
        private Long projectId;
        private String title;
        private String description;
        private String status;
        private String priority;
        private Long assigneeId;
        private String assigneeUsername;
        private String dueDate;
        private Boolean dueSoon;
        private Boolean overdue;
        private String createdAt;
        private String updatedAt;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getProjectId() {
            return projectId;
        }

        public void setProjectId(Long projectId) {
            this.projectId = projectId;
        }

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

        public String getAssigneeUsername() {
            return assigneeUsername;
        }

        public void setAssigneeUsername(String assigneeUsername) {
            this.assigneeUsername = assigneeUsername;
        }

        public String getDueDate() {
            return dueDate;
        }

        public void setDueDate(String dueDate) {
            this.dueDate = dueDate;
        }

        public Boolean getDueSoon() {
            return dueSoon;
        }

        public void setDueSoon(Boolean dueSoon) {
            this.dueSoon = dueSoon;
        }

        public Boolean getOverdue() {
            return overdue;
        }

        public void setOverdue(Boolean overdue) {
            this.overdue = overdue;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }

        public String getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(String updatedAt) {
            this.updatedAt = updatedAt;
        }
    }

    public static class UserDto {
        private Long id;
        private String username;
        private String email;

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

        @Override
        public String toString() {
            return username == null ? "" : username;
        }
    }

    public static class MemberDto {
        private Long userId;
        private String username;
        private String role;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        @Override
        public String toString() {
            return username == null ? "" : username;
        }
    }

    private static class ProjectCreateRequest {
        private String name;
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

    private static class ProjectUpdateRequest {
        private String name;
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

    private static class TaskCreateRequest {
        private String title;
        private String description;
        private String status;
        private String priority;
        private Long assigneeId;
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

    private static class TaskUpdateRequest {
        private String status;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    private static class AddMemberRequest {
        private String usernameOrEmail;

        public String getUsernameOrEmail() {
            return usernameOrEmail;
        }

        public void setUsernameOrEmail(String usernameOrEmail) {
            this.usernameOrEmail = usernameOrEmail;
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
