package com.example.taskmanager.repository;

import com.example.taskmanager.model.entity.ChangeEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChangeEventRepository extends JpaRepository<ChangeEvent, Long> {

    List<ChangeEvent> findTop200ByIdGreaterThanOrderByIdAsc(Long sinceId);
}
