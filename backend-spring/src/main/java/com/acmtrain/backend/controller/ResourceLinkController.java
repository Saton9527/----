package com.acmtrain.backend.controller;

import com.acmtrain.backend.dto.ContestCreateRequest;
import com.acmtrain.backend.dto.ContestResponse;
import com.acmtrain.backend.dto.ProblemsetCreateRequest;
import com.acmtrain.backend.dto.ProblemsetResponse;
import com.acmtrain.backend.dto.UpdateProblemsetSolvedRequest;
import com.acmtrain.backend.service.ResourceLinkService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ResourceLinkController {

    private final ResourceLinkService resourceLinkService;

    public ResourceLinkController(ResourceLinkService resourceLinkService) {
        this.resourceLinkService = resourceLinkService;
    }

    @GetMapping("/problemsets")
    public List<ProblemsetResponse> getProblemsets(@RequestAttribute("userId") Long userId) {
        return resourceLinkService.getProblemsets(userId);
    }

    @PostMapping("/problemsets")
    public ProblemsetResponse createProblemset(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody ProblemsetCreateRequest request
    ) {
        return resourceLinkService.createProblemset(userId, request);
    }

    @PatchMapping("/problemsets/{id}/solved")
    public ProblemsetResponse updateProblemsetSolvedStatus(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateProblemsetSolvedRequest request
    ) {
        return resourceLinkService.updateProblemsetSolvedStatus(userId, id, request);
    }

    @GetMapping("/contests")
    public List<ContestResponse> getContests() {
        return resourceLinkService.getContests();
    }

    @PostMapping("/contests")
    public ContestResponse createContest(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody ContestCreateRequest request
    ) {
        return resourceLinkService.createContest(userId, request);
    }
}