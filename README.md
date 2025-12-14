# Task Manager – Spring Boot & Swing

Collaborative task management with a Spring Boot 2.7.x (Java 8) backend and a Swing desktop client (no external UI libs). Uses SQLite for storage and a simple header-based auth model (`X-USER-ID`).

## Run
Backend (port 8081):
```bash
mvn clean package
mvn spring-boot:run
```
Desktop client (after backend is up):
```bash
mvn exec:java -Dexec.mainClass="com.example.taskmanager.desktop.TaskManagerDesktopApp"
```
SQLite DB `taskmanager.db` is created in the project root; Hibernate `ddl-auto=update` is enabled.

## Features
- Auth: register/login
- Projects: create, update (name/description), delete (owner only), list by membership/ownership
- Members: list, add, remove (owner only, cannot remove owner)
- Tasks: create/list/update/delete; assign to members; due-date picker; status TODO/DOING/DONE
- Desktop UI: CardLayout (Auth → Board), project panel with inline members, edit dialog (details/members/delete), tasks panel with assignee dropdown and date-time picker

## API (key endpoints)
- `POST /api/auth/register`, `POST /api/auth/login`
- `GET /api/projects`, `POST /api/projects`, `PATCH /api/projects/{projectId}`, `DELETE /api/projects/{projectId}`
- `GET /api/projects/{projectId}/members`, `POST /api/projects/{projectId}/members`, `DELETE /api/projects/{projectId}/members/{userId}`
- `GET /api/projects/{projectId}/tasks`, `POST /api/projects/{projectId}/tasks`
- `GET /api/tasks/{taskId}`, `PATCH /api/tasks/{taskId}`, `DELETE /api/tasks/{taskId}`

Errors use a consistent JSON shape `{timestamp,status,error,message,path}`.

## Project Layout
```
src/main/java/com/example/taskmanager/
├── TaskManagerApplication.java
├── config/SQLiteDialect.java
├── controller/ (Auth, Project, Task)
├── desktop/ (DesktopApiClient, TaskManagerDesktopApp, dialogs/panels)
├── exception/ (BadRequestException, NotFoundException, Global handler)
├── model/entity/ (User, Project, ProjectMember, Task)
├── model/dto/ (requests/responses)
├── repository/ (Project, ProjectMember, Task, User)
└── service/ (Auth, Project, Task, User)
src/main/resources/
└── application.yml
```

## Notes
- Auth is header-based for simplicity; add real auth (e.g., Spring Security/JWT) for production.
- Password hashing uses a SHA-256 placeholder; replace with BCrypt for real use.
- Project deletion order: tasks → members → project to avoid SQLite FK issues.
