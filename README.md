# Task Manager – Collaborative Desktop App

A collaborative task management system with a **Java Swing desktop client** and a **Spring Boot backend**, inspired by tools like Trello.  
Supports multi-user collaboration, project/member management, and task tracking with due dates.

---

## ✨ Features

### Authentication

- User registration and login
- Session managed on the desktop client
- Simplified authentication using request header `X-USER-ID`

### Projects

- Create projects
- Edit project name and description
- Delete projects (owner only)
- Project members management (add / remove members)

### Members

- Project owner can:
  - Add members from all users
  - Remove members (except owner)
- Member selection by **username**, not numeric IDs
- Member management integrated into **Edit Project** dialog

### Tasks

- Create tasks under projects
- Assign tasks to project members
- Update task status (TODO / DOING / DONE)
- Delete tasks
- Due date selection via **date + time picker**
- Unassigned tasks supported

### Desktop UI

- Java Swing desktop application
- CardLayout-based navigation (Auth → Board)
- Trello-like layout:
  - Left: Projects
  - Right: Tasks
- Modal dialogs for:
  - Create Project
  - Create Task
  - Edit Project (details + members + delete)

---

## 🧱 Architecture

```
[ Java Swing Desktop App ]
            |
            | REST API (HTTP / JSON)
            |
[ Spring Boot Backend ]
            |
        SQLite Database
```

### Backend

- Java 8
- Spring Boot 2.7.x
- Spring Data JPA
- SQLite
- RESTful API

### Desktop Client

- Java Swing (Java 8)
- SwingWorker for async API calls
- No external UI libraries

---

## 📂 Project Structure (Simplified)

```
.
├── backend
│   ├── controller
│   ├── service
│   ├── repository
│   ├── model
│   └── Application.java
│
└── desktop
    ├── TaskManagerDesktopApp.java
    ├── AuthPanel.java
    ├── BoardPanel.java
    ├── ProjectsListPanel.java
    ├── TasksListPanel.java
    ├── EditProjectDialog.java
    ├── CreateProjectDialog.java
    ├── CreateTaskDialog.java
    └── DesktopApiClient.java
```

---

## 🚀 Getting Started

### 1) Backend Setup

#### Prerequisites

- Java 8+
- Maven

#### Run Backend

```bash
cd backend
mvn clean package
mvn spring-boot:run
```

Backend runs at:

```
http://localhost:8081
```

---

### 2) Desktop App Setup

#### Run Desktop Client

```bash
cd desktop
javac com/example/taskmanager/desktop/TaskManagerDesktopApp.java
java com.example.taskmanager.desktop.TaskManagerDesktopApp
```

Or run directly from your IDE.

---

## 🧪 Demo Flow

1. Launch Desktop App
2. Register a new user
3. Login
4. Create a project
5. Edit project:
   - Change name / description
   - Add or remove members
6. Create tasks:
   - Assign to members
   - Set due date
7. Update task status
8. Delete task or project

---

## 🔐 Authorization Rules

- Only project owner can:
  - Edit project details
  - Add/remove members
  - Delete project
- Members can:
  - View projects
  - Create and update tasks

---

## 🗑️ Delete Behavior

- Deleting a project also deletes:
  - All tasks under the project
  - All project members
- Delete order handled safely to avoid SQLite foreign key constraints:
  ```
  Tasks → Project Members → Project
  ```

---

## 🛠️ Technologies Used

- Java 8
- Spring Boot
- Spring Data JPA
- SQLite
- Java Swing
- Maven

---

## 📌 Design Highlights

- Clear separation between backend and desktop client
- Asynchronous UI updates using SwingWorker
- User-friendly UI (no ID-based inputs)
- Owner-based authorization enforcement
- Extendable architecture for future features

---

## 🔮 Future Improvements

- Task filters (due soon / overdue)
- Drag-and-drop task board
- Project archiving (soft delete)
- Notifications for upcoming due dates
- Role-based permissions

---

## 👤 Author

**Ta-Chun Tai**, **You-Chun Luo**
Java / Backend / Desktop Application Developer
