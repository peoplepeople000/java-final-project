package com.example.taskmanager.service;

import com.example.taskmanager.model.entity.ChangeEvent;
import com.example.taskmanager.repository.ChangeEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChangeEventService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChangeEventService.class);
    private final ChangeEventRepository changeEventRepository;

    public ChangeEventService(ChangeEventRepository changeEventRepository) {
        this.changeEventRepository = changeEventRepository;
    }

    @Transactional
    public void record(String type, Long entityId, Long projectId) {
        try {
            ChangeEvent event = new ChangeEvent();
            event.setType(type);
            event.setEntityId(entityId);
            event.setProjectId(projectId);
            changeEventRepository.save(event);
        } catch (Exception ex) {
            // Do not block main flows; just log the failure.
            LOGGER.warn("Failed to record change event: type={}, entityId={}, projectId={}, error={}",
                    type, entityId, projectId, ex.getMessage());
        }
    }
}
