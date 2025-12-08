package com.example.taskmanager.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.taskmanager.model.entity.Task;

public interface TaskRepository extends JpaRepository<Task, Long> {

    // TODO: add query methods for filtering by status, priority, and due dates

    List<Task> findByProjectId(Long projectId);
}
