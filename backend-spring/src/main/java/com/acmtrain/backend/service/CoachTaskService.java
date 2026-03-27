package com.acmtrain.backend.service;

import com.acmtrain.backend.dto.CoachTaskCreateRequest;
import com.acmtrain.backend.dto.CoachTaskResponse;
import com.acmtrain.backend.dto.MyCoachTaskResponse;
import com.acmtrain.backend.dto.UpdateAssignmentStatusRequest;
import com.acmtrain.backend.entity.CoachTaskAssignmentEntity;
import com.acmtrain.backend.entity.CoachTaskEntity;
import com.acmtrain.backend.entity.TeamEntity;
import com.acmtrain.backend.entity.TeamMemberEntity;
import com.acmtrain.backend.entity.UserAccountEntity;
import com.acmtrain.backend.repository.CoachTaskAssignmentRepository;
import com.acmtrain.backend.repository.CoachTaskRepository;
import com.acmtrain.backend.repository.TeamMemberRepository;
import com.acmtrain.backend.repository.TeamRepository;
import com.acmtrain.backend.repository.UserAccountRepository;
import com.acmtrain.backend.service.dto.DtoMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class CoachTaskService {

    private static final DateTimeFormatter DATETIME_INPUT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATETIME_OUTPUT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final CoachTaskRepository coachTaskRepository;
    private final CoachTaskAssignmentRepository coachTaskAssignmentRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserAccountRepository userAccountRepository;

    public CoachTaskService(
            CoachTaskRepository coachTaskRepository,
            CoachTaskAssignmentRepository coachTaskAssignmentRepository,
            TeamRepository teamRepository,
            TeamMemberRepository teamMemberRepository,
            UserAccountRepository userAccountRepository
    ) {
        this.coachTaskRepository = coachTaskRepository;
        this.coachTaskAssignmentRepository = coachTaskAssignmentRepository;
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional
    public CoachTaskResponse createTask(Long userId, CoachTaskCreateRequest request) {
        UserAccountEntity coach = mustFindUser(userId);
        if (!"coach".equalsIgnoreCase(coach.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有教练可以下发任务");
        }

        TeamEntity team = mustFindTeam(request.teamId());
        if (team.getCoachId() == null || !team.getCoachId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只能给自己名下队伍下发任务");
        }

        List<TeamMemberEntity> members = teamMemberRepository.findByTeamIdOrderByIdAsc(team.getId());
        if (members.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "队伍无成员，无法下发任务");
        }

        LocalDateTime now = LocalDateTime.now();
        CoachTaskEntity task = new CoachTaskEntity();
        task.setCoachId(userId);
        task.setTeamId(team.getId());
        task.setTitle(request.title().trim());
        task.setDescription(request.description().trim());
        task.setDeadline(parseDateTime(request.deadline()));
        task.setCreatedAt(now);
        CoachTaskEntity savedTask = coachTaskRepository.save(task);

        for (TeamMemberEntity member : members) {
            CoachTaskAssignmentEntity assignment = new CoachTaskAssignmentEntity();
            assignment.setTaskId(savedTask.getId());
            assignment.setUserId(member.getUserId());
            assignment.setStatus("ASSIGNED");
            assignment.setCreatedAt(now);
            coachTaskAssignmentRepository.save(assignment);
        }

        return DtoMapper.toCoachTaskResponse(savedTask);
    }

    public List<CoachTaskResponse> getMyCreatedTasks(Long userId) {
        UserAccountEntity coach = mustFindUser(userId);
        if (!"coach".equalsIgnoreCase(coach.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有教练可以查看已下发任务");
        }

        return coachTaskRepository.findByCoachIdOrderByIdDesc(userId)
                .stream()
                .map(DtoMapper::toCoachTaskResponse)
                .toList();
    }

    public List<MyCoachTaskResponse> getMyAssignments(Long userId) {
        return coachTaskAssignmentRepository.findByUserIdOrderByIdDesc(userId)
                .stream()
                .map(this::toMyAssignmentResponse)
                .toList();
    }

    @Transactional
    public MyCoachTaskResponse updateMyAssignmentStatus(Long userId, Long assignmentId, UpdateAssignmentStatusRequest request) {
        CoachTaskAssignmentEntity assignment = coachTaskAssignmentRepository.findByIdAndUserId(assignmentId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "任务分配不存在"));

        String nextStatus = normalizeStatus(request.status());
        assignment.setStatus(nextStatus);
        if ("DONE".equals(nextStatus)) {
            assignment.setCompletedAt(LocalDateTime.now());
        } else {
            assignment.setCompletedAt(null);
        }

        CoachTaskAssignmentEntity saved = coachTaskAssignmentRepository.save(assignment);
        return toMyAssignmentResponse(saved);
    }

    private String normalizeStatus(String status) {
        String normalized = status.trim().toUpperCase();
        if (!"ASSIGNED".equals(normalized) && !"IN_PROGRESS".equals(normalized) && !"DONE".equals(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status 仅支持 ASSIGNED/IN_PROGRESS/DONE");
        }
        return normalized;
    }

    private MyCoachTaskResponse toMyAssignmentResponse(CoachTaskAssignmentEntity assignment) {
        CoachTaskEntity task = coachTaskRepository.findById(assignment.getTaskId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "任务不存在"));

        UserAccountEntity coach = mustFindUser(task.getCoachId());
        return new MyCoachTaskResponse(
                assignment.getId(),
                task.getId(),
                task.getTeamId(),
                task.getTitle(),
                task.getDescription(),
                task.getDeadline().format(DATETIME_OUTPUT),
                assignment.getStatus(),
                coach.getRealName()
        );
    }

    private TeamEntity mustFindTeam(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "队伍不存在"));
    }

    private UserAccountEntity mustFindUser(Long userId) {
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户不存在"));
    }

    private LocalDateTime parseDateTime(String input) {
        try {
            return LocalDateTime.parse(input, DATETIME_INPUT);
        } catch (DateTimeParseException ex) {
            try {
                return LocalDateTime.parse(input);
            } catch (DateTimeParseException ignored) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "deadline 格式错误，应为 yyyy-MM-dd HH:mm");
            }
        }
    }
}