package com.example.taskmanager.controller;

import com.example.taskmanager.exception.BadRequestException;
import com.example.taskmanager.model.dto.change.ChangeEventDto;
import com.example.taskmanager.repository.ChangeEventRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/changes")
public class ChangeEventController {

    private final ChangeEventRepository changeEventRepository;

    public ChangeEventController(ChangeEventRepository changeEventRepository) {
        this.changeEventRepository = changeEventRepository;
    }

    @GetMapping
    public List<ChangeEventDto> listChanges(
            @RequestHeader(value = "X-USER-ID", required = false) String userIdHeader,
            @RequestParam(value = "since", required = false, defaultValue = "0") Long since) {
        parseUserId(userIdHeader); // for now, we only validate presence
        if (since == null || since < 0) {
            throw new BadRequestException("Invalid since");
        }
        List<ChangeEventDto> events = changeEventRepository.findTop200ByIdGreaterThanOrderByIdAsc(since).stream()
                .map(ChangeEventDto::from)
                .collect(Collectors.toList());
        System.out.println("[ChangeEventController] since=" + since + " returning " + events.size() + " events");
        return events;
    }

    private Long parseUserId(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.trim().isEmpty()) {
            throw new BadRequestException("X-USER-ID header is required");
        }
        try {
            return Long.valueOf(userIdHeader.trim());
        } catch (NumberFormatException ex) {
            throw new BadRequestException("X-USER-ID header must be a valid number");
        }
    }
}
