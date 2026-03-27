package com.acmtrain.backend.controller;

import com.acmtrain.backend.dto.CoachTaskCreateRequest;
import com.acmtrain.backend.dto.CoachTaskResponse;
import com.acmtrain.backend.dto.MyCoachTaskResponse;
import com.acmtrain.backend.dto.UpdateAssignmentStatusRequest;
import com.acmtrain.backend.service.CoachTaskService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coach/tasks")
public class CoachTaskController {

    private final CoachTaskService coachTaskService;

    public CoachTaskController(CoachTaskService coachTaskService) {
        this.coachTaskService = coachTaskService;
    }

    @PostMapping
    public CoachTaskResponse createTask(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody CoachTaskCreateRequest request
    ) {
        return coachTaskService.createTask(userId, request);
    }

    @GetMapping("/my-created")
    public List<CoachTaskResponse> getMyCreatedTasks(@RequestAttribute("userId") Long userId) {
        return coachTaskService.getMyCreatedTasks(userId);
    }

    @GetMapping("/my-assignments")
    public List<MyCoachTaskResponse> getMyAssignments(@RequestAttribute("userId") Long userId) {
        return coachTaskService.getMyAssignments(userId);
    }

    @PatchMapping("/assignments/{assignmentId}/status")
    public MyCoachTaskResponse updateAssignmentStatus(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long assignmentId,
            @Valid @RequestBody UpdateAssignmentStatusRequest request
    ) {
        return coachTaskService.updateMyAssignmentStatus(userId, assignmentId, request);
    }
}