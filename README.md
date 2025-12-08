# Task Manager Backend & Clients

A Spring Boot 2.7.x application that exposes a collaborative task manager API powered by SQLite. The repository also includes a Swing desktop client for quick end-to-end testing.

## Tech Stack
- Java 8
- Spring Boot 2.7.x (Web, Data JPA, Validation, Actuator)
- SQLite (via `org.xerial:sqlite-jdbc`)
- Maven build
- Swing desktop UI

## Features
- User management with registration and simple login (passwords hashed via SHA-256 placeholder)
- Entities for `User`, `Project`, `ProjectMember`, and `Task`
- Project APIs: create project, list projects for a user, add/list project members
- Task APIs: create tasks per project, list project tasks, update task status
- Global exception handling with consistent JSON error payloads
- Desktop (Swing) client for register/login proof of concept

## Project Structure
```
src/main/java/com/example/taskmanager/
├── TaskManagerApplication.java
├── config/SQLiteDialect.java
├── controller/
│   ├── AuthController.java
│   ├── ProjectController.java
│   └── TaskController.java
├── desktop/
│   ├── DesktopApiClient.java
│   └── TaskManagerDesktopApp.java
├── exception/...
├── model/entity/...
├── repository/...
└── service/
    ├── AuthService.java
    ├── ProjectService.java
    ├── TaskService.java
    └── UserService.java
src/main/resources/
└── application.yml
```

## Getting Started
### Prerequisites
- Java 8 JDK installed and available on `PATH`
- Maven 3.6+ installed

### Build & Run
```
mvn clean package
mvn spring-boot:run
```
The application listens on `http://localhost:8081` (configured in `application.yml`).

SQLite database file `taskmanager.db` will be created in the project root. Default schema management uses Hibernate `ddl-auto=update`.

### REST Endpoints
- `POST /api/auth/register` – body `{ "username", "email", "password" }`
- `POST /api/auth/login` – body `{ "usernameOrEmail", "password" }`
- `POST /api/projects` – create project (owner ID)
- `GET /api/projects/user/{userId}` – projects where user is owner or member
- `POST /api/projects/{projectId}/members` – add member
- `GET /api/projects/{projectId}/members`
- `POST /api/projects/{projectId}/tasks`
- `GET /api/projects/{projectId}/tasks`
- `PATCH /api/tasks/{taskId}/status`

All error responses follow `{ timestamp, status, error, message, path }`.

## Desktop Client
`TaskManagerDesktopApp` (Swing) provides register/login tabs and communicates with the backend through `DesktopApiClient`. Run it from your IDE or via:
```
mvn exec:java -Dexec.mainClass="com.example.taskmanager.desktop.TaskManagerDesktopApp"
```
Ensure the backend is running first.

## Notes
- Password hashing currently uses SHA-256 for simplicity. Replace with BCrypt or another strong algorithm for production.
- Authentication is deliberately simple (no tokens/sessions). Extend with Spring Security or JWT for real deployments.
- SQLite schema modifications are limited; consider using Flyway or manual migrations for complex changes.
