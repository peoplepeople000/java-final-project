package com.example.taskmanager.repository;

import com.example.taskmanager.model.entity.Project;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findById(Long id);

    List<Project> findByOwnerId(Long ownerId);
}
