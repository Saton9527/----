package com.acmtrain.backend.service;

import com.acmtrain.backend.dto.CoachTaskCreateRequest;
import com.acmtrain.backend.dto.CoachTaskAssigneeResponse;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CoachTaskService {

    private static final Logger logger = LoggerFactory.getLogger(CoachTaskService.class);
    private static final DateTimeFormatter DATETIME_INPUT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATETIME_OUTPUT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final CoachTaskRepository coachTaskRepository;
    private final CoachTaskAssignmentRepository coachTaskAssignmentRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserAccountRepository userAccountRepository;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final boolean mailEnabled;
    private final String mailFrom;

    public CoachTaskService(
            CoachTaskRepository coachTaskRepository,
            CoachTaskAssignmentRepository coachTaskAssignmentRepository,
            TeamRepository teamRepository,
            TeamMemberRepository teamMemberRepository,
            UserAccountRepository userAccountRepository,
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${acm.coach-task.mail.enabled:false}") boolean mailEnabled,
            @Value("${acm.coach-task.mail.from:${MAIL_FROM:no-reply@acmtrain.local}}") String mailFrom
    ) {
        this.coachTaskRepository = coachTaskRepository;
        this.coachTaskAssignmentRepository = coachTaskAssignmentRepository;
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.userAccountRepository = userAccountRepository;
        this.mailSenderProvider = mailSenderProvider;
        this.mailEnabled = mailEnabled;
        this.mailFrom = mailFrom;
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

        List<TeamMemberEntity> assignees = resolveAssignees(request.assigneeUserIds(), members);
        LocalDateTime now = LocalDateTime.now();
        CoachTaskEntity task = new CoachTaskEntity();
        task.setCoachId(userId);
        task.setTeamId(team.getId());
        task.setTitle(request.title().trim());
        task.setDescription(request.description().trim());
        task.setDeadline(parseDateTime(request.deadline()));
        task.setCreatedAt(now);
        CoachTaskEntity savedTask = coachTaskRepository.save(task);

        for (TeamMemberEntity member : assignees) {
            CoachTaskAssignmentEntity assignment = new CoachTaskAssignmentEntity();
            assignment.setTaskId(savedTask.getId());
            assignment.setUserId(member.getUserId());
            assignment.setStatus("ASSIGNED");
            assignment.setCreatedAt(now);
            coachTaskAssignmentRepository.save(assignment);
        }

        sendAssignmentEmailsIfEnabled(coach, team, savedTask, assignees);

        return toCoachTaskResponse(savedTask, team, assignees);
    }

    public List<CoachTaskResponse> getMyCreatedTasks(Long userId) {
        UserAccountEntity coach = mustFindUser(userId);
        if (!"coach".equalsIgnoreCase(coach.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有教练可以查看已下发任务");
        }

        return coachTaskRepository.findByCoachIdOrderByIdDesc(userId)
                .stream()
                .map(this::toCoachTaskResponse)
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

    private CoachTaskResponse toCoachTaskResponse(CoachTaskEntity task) {
        TeamEntity team = mustFindTeam(task.getTeamId());
        List<TeamMemberEntity> members = teamMemberRepository.findByTeamIdOrderByIdAsc(team.getId());
        return toCoachTaskResponse(task, team, members);
    }

    private CoachTaskResponse toCoachTaskResponse(CoachTaskEntity task, TeamEntity team, List<TeamMemberEntity> members) {
        List<CoachTaskAssignmentEntity> assignments = coachTaskAssignmentRepository.findByTaskIdOrderByIdAsc(task.getId());
        Map<Long, TeamMemberEntity> memberByUserId = members.stream()
                .collect(Collectors.toMap(TeamMemberEntity::getUserId, member -> member, (left, right) -> left));
        List<CoachTaskAssigneeResponse> assignees = new ArrayList<>();
        int inProgressCount = 0;
        int doneCount = 0;

        for (CoachTaskAssignmentEntity assignment : assignments) {
            TeamMemberEntity member = memberByUserId.get(assignment.getUserId());
            UserAccountEntity user = member == null ? mustFindUser(assignment.getUserId()) : mustFindUser(member.getUserId());
            if ("IN_PROGRESS".equalsIgnoreCase(assignment.getStatus())) {
                inProgressCount++;
            }
            if ("DONE".equalsIgnoreCase(assignment.getStatus())) {
                doneCount++;
            }
            assignees.add(new CoachTaskAssigneeResponse(
                    assignment.getId(),
                    user.getId(),
                    user.getUsername(),
                    user.getRealName(),
                    assignment.getStatus(),
                    assignment.getCompletedAt() == null ? null : assignment.getCompletedAt().format(DATETIME_OUTPUT)
            ));
        }

        return new CoachTaskResponse(
                task.getId(),
                task.getTeamId(),
                team.getName(),
                task.getTitle(),
                task.getDescription(),
                task.getDeadline().format(DATETIME_OUTPUT),
                task.getCreatedAt().format(DATETIME_OUTPUT),
                assignments.size(),
                inProgressCount,
                doneCount,
                assignees
        );
    }

    private List<TeamMemberEntity> resolveAssignees(List<Long> requestedUserIds, List<TeamMemberEntity> members) {
        if (requestedUserIds == null || requestedUserIds.isEmpty()) {
            return members;
        }

        Set<Long> selectedUserIds = new LinkedHashSet<>(requestedUserIds);
        List<TeamMemberEntity> matchedMembers = members.stream()
                .filter(member -> selectedUserIds.contains(member.getUserId()))
                .toList();
        if (matchedMembers.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请至少选择一名有效队员");
        }
        if (matchedMembers.size() != selectedUserIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "下发对象必须全部来自当前队伍");
        }
        return matchedMembers;
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

    private void sendAssignmentEmailsIfEnabled(
            UserAccountEntity coach,
            TeamEntity team,
            CoachTaskEntity task,
            List<TeamMemberEntity> assignees
    ) {
        if (!mailEnabled) {
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            logger.warn("Coach task mail is enabled but JavaMailSender is unavailable.");
            return;
        }

        for (TeamMemberEntity assignee : assignees) {
            UserAccountEntity targetUser = mustFindUser(assignee.getUserId());
            String email = targetUser.getEmail();
            if (email == null || email.isBlank()) {
                continue;
            }

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(email.trim());
            message.setSubject("[ACM Train] 新教练任务：" + task.getTitle());
            message.setText(buildAssignmentMailBody(coach, team, task, targetUser));
            try {
                mailSender.send(message);
            } catch (Exception ex) {
                logger.warn("Failed to send coach task mail, taskId={}, userId={}", task.getId(), targetUser.getId(), ex);
            }
        }
    }

    private String buildAssignmentMailBody(
            UserAccountEntity coach,
            TeamEntity team,
            CoachTaskEntity task,
            UserAccountEntity targetUser
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append(targetUser.getRealName()).append(" 你好，\n\n");
        builder.append("你收到一条新的教练任务，请及时登录系统查看并处理。\n\n");
        builder.append("队伍：").append(team.getName()).append('\n');
        builder.append("教练：").append(coach.getRealName()).append('\n');
        builder.append("任务标题：").append(task.getTitle()).append('\n');
        builder.append("任务描述：").append(task.getDescription()).append('\n');
        builder.append("截止时间：").append(task.getDeadline().format(DATETIME_OUTPUT)).append('\n');
        builder.append('\n');
        builder.append("请登录 ACM Train 查看任务详情。");
        return builder.toString();
    }
}
