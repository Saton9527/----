package com.acmtrain.backend.controller;

import com.acmtrain.backend.dto.TeamCoachRequest;
import com.acmtrain.backend.dto.TeamCreateRequest;
import com.acmtrain.backend.dto.TeamInviteRequest;
import com.acmtrain.backend.dto.TeamInviteResponse;
import com.acmtrain.backend.dto.TeamResponse;
import com.acmtrain.backend.service.TeamService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @PostMapping("/teams")
    public TeamResponse createTeam(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody TeamCreateRequest request
    ) {
        return teamService.createTeam(userId, request);
    }

    @GetMapping("/teams/me")
    public TeamResponse getMyTeam(@RequestAttribute("userId") Long userId) {
        return teamService.getMyTeam(userId);
    }

    @GetMapping("/teams/coach/me")
    public List<TeamResponse> getCoachTeams(@RequestAttribute("userId") Long userId) {
        return teamService.getCoachTeams(userId);
    }

    @PostMapping("/teams/{teamId}/invites")
    public TeamInviteResponse inviteMember(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long teamId,
            @Valid @RequestBody TeamInviteRequest request
    ) {
        return teamService.inviteMember(userId, teamId, request);
    }

    @GetMapping("/teams/invites/me")
    public List<TeamInviteResponse> getMyPendingInvites(@RequestAttribute("userId") Long userId) {
        return teamService.getMyPendingInvites(userId);
    }

    @PostMapping("/teams/invites/{inviteId}/accept")
    public TeamResponse acceptInvite(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long inviteId
    ) {
        return teamService.acceptInvite(userId, inviteId);
    }

    @PostMapping("/teams/invites/{inviteId}/reject")
    public TeamInviteResponse rejectInvite(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long inviteId
    ) {
        return teamService.rejectInvite(userId, inviteId);
    }

    @PostMapping("/teams/{teamId}/coach")
    public TeamResponse assignCoach(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long teamId,
            @Valid @RequestBody TeamCoachRequest request
    ) {
        return teamService.assignCoach(userId, teamId, request);
    }
}