# Task Manager – Spring Boot Backend & Swing Desktop Client

A complete collaborative task manager built as our Java final project. The backend is a Spring Boot 2.7.x (Java 8) service that exposes REST APIs and WebSocket notifications. The desktop application is written entirely in Swing (no external UI toolkits) and consumes those APIs for authentication, project/task management, and real‑time updates. SQLite provides lightweight persistence, making the project easy to run everywhere.

## Highlights of the System

### Backend
- **Spring Boot REST API**  
  - Authentication: register & login endpoints with validation.  
  - Projects: create, update, delete (owner only), list all projects owned or joined.  
  - Members: list members, invite new members, remove members (owner only).  
  - Tasks: CRUD with assignment, priority, due date, and status (TODO/DOING/DONE).  
  - Consistent error envelope `{timestamp,status,error,message,path}` via `GlobalExceptionHandler`.
- **WebSocket Push Updates**  
  - `spring-boot-starter-websocket` with STOMP endpoints (`/ws`) and simple broker (`/topic/...`).  
  - `ProjectService` / `TaskService` publish `ProjectEvent` / `TaskEvent` on any mutation so connected clients refresh immediately, eliminating polling.  
- **Data Layer**  
  - Entities: `User`, `Project`, `ProjectMember`, `Task` with auditing timestamps via JPA lifecycle hooks.  
  - `SQLiteDialect` / `SQLiteIdentityColumnSupport` bridge Hibernate with SQLite.  
  - Repositories + services encapsulate ownership checks, membership validation, and cascade deletes.

### Desktop Client (Swing)
- **Auth Panel**: Tabbed UI for register/login (Enter key submits), status labels with descriptive error messages.  
- **Board View**: `CardLayout` toggles between auth and the main board.  
  - Left: `ProjectsListPanel` shows projects plus owner info, inline member list, context menu (edit/delete).  
  - Right: `TasksListPanel` renders a mini Kanban (TODO/DOING/DONE columns). Only one task is selectable at a time; double-click edits; context menu lets you move status or delete.  
- **Dialogs**: `CreateProjectDialog`, `EditProjectDialog`, `CreateTaskDialog`, `EditTaskDialog` with larger default sizes, user/member pickers, and validation messages.  
- **Realtime Updates**:  
  - `RealtimeUpdateClient` opens a STOMP WebSocket connection (`ws://localhost:8081/ws`).  
  - Projects/tasks refresh instantly when other users change data, without timers that cause UI flashing.  
  - Local actions skip redundant refresh events.

### Advanced Concepts Covered
- Swing GUI, multi-panel layouts, custom renderers, modal dialogs, mouse listeners.  
- REST + WebSocket networking (HTTP & STOMP).  
- SQLite database + JPA/Hibernate + custom dialect.  
- Background work with `SwingWorker` to keep UI responsive.  
- Realtime collaboration scenario with optimistic UI updates.

## Running the Project
1. **Backend (port 8081)**  
   ```bash
   mvn clean package
   mvn spring-boot:run
   ```  
   Creates `taskmanager.db` in the project root (`ddl-auto=update`).

2. **Desktop client (after backend is running)**  
   ```bash
   mvn exec:java -Dexec.mainClass="com.example.taskmanager.desktop.TaskManagerDesktopApp"
   ```  
   Launch multiple instances to simulate different users; WebSocket updates keep them in sync.

## API Overview
- Auth: `POST /api/auth/register`, `POST /api/auth/login` (returns user info).  
- Projects: `GET /api/projects`, `POST /api/projects`, `PATCH /api/projects/{id}`, `DELETE /api/projects/{id}`.  
- Members: `GET /api/projects/{id}/members`, `POST /api/projects/{id}/members`, `DELETE /api/projects/{id}/members/{userId}`.  
- Tasks:  
  - `GET /api/projects/{projectId}/tasks`, `POST /api/projects/{projectId}/tasks`.  
  - `GET /api/tasks/{taskId}`, `PATCH /api/tasks/{taskId}`, `DELETE /api/tasks/{taskId}`.  
- WebSocket: clients subscribe to `/topic/projects` and `/topic/tasks` for `ProjectEvent` / `TaskEvent`.

All modifying endpoints require an `X-USER-ID` header (the user id returned from login). This keeps the focus on application logic instead of implementing a full token-based authentication flow.

## Project Structure
```
src/main/java/com/example/taskmanager/
├── TaskManagerApplication.java
├── config/
│   ├── SQLiteDialect.java
│   ├── SQLiteIdentityColumnSupport.java
│   └── WebSocketConfig.java
├── controller/
│   ├── AuthController
│   ├── ProjectController
│   ├── TaskController
│   └── UserController
├── desktop/
│   ├── TaskManagerDesktopApp, BoardPanel, AuthPanel, ...
│   ├── dialogs (Create/Edit Project/Task)
│   ├── RealtimeUpdateClient (STOMP over WebSocket)
│   └── DesktopApiClient (REST wrapper)
├── exception/ (BadRequestException, NotFoundException, Global handler)
├── model/
│   ├── entity (User, Project, ProjectMember, Task)
│   ├── dto (requests/responses)
│   └── event (ProjectEvent, TaskEvent)
├── repository/ (UserRepository, ProjectRepository, ProjectMemberRepository, TaskRepository)
└── service/ (AuthService, ProjectService, TaskService, UserService)
src/main/resources/
└── application.yml
```

## Notes & Possible Improvements
- Authentication currently uses a simple header; replacing it with Spring Security / JWT would harden the app.  
- Passwords are hashed with SHA‑256 for simplicity; use BCrypt or Argon2 for production.  
- Task filtering (status / assignee) is partially scaffolded and can be expanded.  
- WebSocket security is open (`allowedOriginPatterns("*")`) for easy testing; consider tightening in production environments.

This README captures the architecture, features, and setup instructions so reviewers can quickly understand the scope of the project and run it locally. We've intentionally explored multiple advanced Java topics (GUI, Networking, Database, WebSocket) to demonstrate what we learned throughout the course.*** End Patch
