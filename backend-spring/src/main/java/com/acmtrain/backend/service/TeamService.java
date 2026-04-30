package com.acmtrain.backend.service;

import com.acmtrain.backend.dto.TeamCoachRequest;
import com.acmtrain.backend.dto.TeamCreateRequest;
import com.acmtrain.backend.dto.TeamInviteRequest;
import com.acmtrain.backend.dto.TeamInviteResponse;
import com.acmtrain.backend.dto.TeamMemberResponse;
import com.acmtrain.backend.dto.TeamResponse;
import com.acmtrain.backend.entity.TeamEntity;
import com.acmtrain.backend.entity.TeamInviteEntity;
import com.acmtrain.backend.entity.TeamMemberEntity;
import com.acmtrain.backend.entity.UserAccountEntity;
import com.acmtrain.backend.repository.TeamInviteRepository;
import com.acmtrain.backend.repository.TeamMemberRepository;
import com.acmtrain.backend.repository.TeamRepository;
import com.acmtrain.backend.repository.UserAccountRepository;
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
import java.util.List;
import java.util.Optional;

@Service
public class TeamService {

    private static final Logger logger = LoggerFactory.getLogger(TeamService.class);
    private static final DateTimeFormatter DATETIME_OUTPUT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String CAPTAIN = "CAPTAIN";
    private static final String MEMBER = "MEMBER";
    private static final String PENDING = "PENDING";
    private static final String ACCEPTED = "ACCEPTED";
    private static final String REJECTED = "REJECTED";

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamInviteRepository teamInviteRepository;
    private final UserAccountRepository userAccountRepository;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final boolean inviteMailEnabled;
    private final String inviteMailFrom;

    public TeamService(
            TeamRepository teamRepository,
            TeamMemberRepository teamMemberRepository,
            TeamInviteRepository teamInviteRepository,
            UserAccountRepository userAccountRepository,
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${acm.team.mail.enabled:false}") boolean inviteMailEnabled,
            @Value("${acm.team.mail.from:${MAIL_FROM:no-reply@acmtrain.local}}") String inviteMailFrom
    ) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.teamInviteRepository = teamInviteRepository;
        this.userAccountRepository = userAccountRepository;
        this.mailSenderProvider = mailSenderProvider;
        this.inviteMailEnabled = inviteMailEnabled;
        this.inviteMailFrom = inviteMailFrom;
    }

    @Transactional
    public TeamResponse createTeam(Long userId, TeamCreateRequest request) {
        UserAccountEntity currentUser = mustFindUser(userId);
        if (!"student".equalsIgnoreCase(currentUser.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有学生可以发起组队");
        }
        if (teamMemberRepository.findByUserId(userId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "你已经加入了队伍");
        }

        String teamName = request.name().trim();
        if (teamRepository.existsByName(teamName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "队伍名称已存在");
        }

        LocalDateTime now = LocalDateTime.now();
        TeamEntity team = new TeamEntity();
        team.setName(teamName);
        team.setCreatedAt(now);
        TeamEntity savedTeam = teamRepository.save(team);

        TeamMemberEntity captain = new TeamMemberEntity();
        captain.setTeamId(savedTeam.getId());
        captain.setUserId(userId);
        captain.setMemberRole(CAPTAIN);
        captain.setCreatedAt(now);
        teamMemberRepository.save(captain);

        return buildTeamResponse(savedTeam);
    }

    public TeamResponse getMyTeam(Long userId) {
        Optional<TeamMemberEntity> membership = teamMemberRepository.findByUserId(userId);
        if (membership.isEmpty()) {
            return null;
        }
        TeamEntity team = mustFindTeam(membership.get().getTeamId());
        return buildTeamResponse(team);
    }

    public List<TeamResponse> getCoachTeams(Long userId) {
        UserAccountEntity currentUser = mustFindUser(userId);
        if (!"coach".equalsIgnoreCase(currentUser.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有教练可以查看名下队伍");
        }
        return teamRepository.findByCoachIdOrderByIdDesc(userId)
                .stream()
                .map(this::buildTeamResponse)
                .toList();
    }

    @Transactional
    public TeamInviteResponse inviteMember(Long userId, Long teamId, TeamInviteRequest request) {
        ensureCaptain(userId, teamId);
        if (teamMemberRepository.countByTeamId(teamId) >= 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "队伍人数已满（最多 3 人）");
        }

        UserAccountEntity invitee = userAccountRepository.findByUsername(request.username().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "被邀请用户不存在"));

        if (invitee.getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不能邀请自己");
        }
        if (!"student".equalsIgnoreCase(invitee.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "只能邀请学生作为队员");
        }
        if (teamMemberRepository.findByUserId(invitee.getId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该用户已加入其他队伍");
        }
        if (teamInviteRepository.findByTeamIdAndInviteeIdAndStatus(teamId, invitee.getId(), PENDING).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "已向该用户发送过邀请");
        }

        TeamInviteEntity invite = new TeamInviteEntity();
        invite.setTeamId(teamId);
        invite.setInviterId(userId);
        invite.setInviteeId(invitee.getId());
        invite.setStatus(PENDING);
        invite.setCreatedAt(LocalDateTime.now());
        TeamInviteEntity savedInvite = teamInviteRepository.save(invite);
        TeamEntity team = mustFindTeam(teamId);
        UserAccountEntity inviter = mustFindUser(userId);
        sendInviteEmailIfEnabled(team, inviter, invitee, savedInvite);

        return toInviteResponse(savedInvite);
    }

    public List<TeamInviteResponse> getMyPendingInvites(Long userId) {
        return teamInviteRepository.findByInviteeIdAndStatusOrderByIdDesc(userId, PENDING)
                .stream()
                .map(this::toInviteResponse)
                .toList();
    }

    @Transactional
    public TeamResponse acceptInvite(Long userId, Long inviteId) {
        TeamInviteEntity invite = teamInviteRepository.findById(inviteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "邀请不存在"));

        if (!invite.getInviteeId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "不能处理他人的邀请");
        }
        if (!PENDING.equalsIgnoreCase(invite.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该邀请已处理");
        }
        if (teamMemberRepository.findByUserId(userId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "你已经加入了其他队伍");
        }
        if (teamMemberRepository.countByTeamId(invite.getTeamId()) >= 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "队伍人数已满");
        }

        TeamEntity team = mustFindTeam(invite.getTeamId());

        TeamMemberEntity newMember = new TeamMemberEntity();
        newMember.setTeamId(team.getId());
        newMember.setUserId(userId);
        newMember.setMemberRole(MEMBER);
        newMember.setCreatedAt(LocalDateTime.now());
        teamMemberRepository.save(newMember);

        invite.setStatus(ACCEPTED);
        teamInviteRepository.save(invite);

        List<TeamInviteEntity> otherInvites = teamInviteRepository.findByInviteeIdAndStatusOrderByIdDesc(userId, PENDING);
        for (TeamInviteEntity pendingInvite : otherInvites) {
            if (!pendingInvite.getId().equals(inviteId)) {
                pendingInvite.setStatus(REJECTED);
                teamInviteRepository.save(pendingInvite);
            }
        }

        return buildTeamResponse(team);
    }

    @Transactional
    public TeamInviteResponse rejectInvite(Long userId, Long inviteId) {
        TeamInviteEntity invite = teamInviteRepository.findById(inviteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "邀请不存在"));

        if (!invite.getInviteeId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "不能处理他人的邀请");
        }
        if (!PENDING.equalsIgnoreCase(invite.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该邀请已处理");
        }

        invite.setStatus(REJECTED);
        TeamInviteEntity savedInvite = teamInviteRepository.save(invite);
        return toInviteResponse(savedInvite);
    }

    @Transactional
    public TeamResponse assignCoach(Long userId, Long teamId, TeamCoachRequest request) {
        ensureCaptain(userId, teamId);
        TeamEntity team = mustFindTeam(teamId);

        UserAccountEntity coach = userAccountRepository.findByUsername(request.coachUsername().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "教练用户不存在"));
        if (!"coach".equalsIgnoreCase(coach.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "指定账号不是教练角色");
        }

        team.setCoachId(coach.getId());
        TeamEntity savedTeam = teamRepository.save(team);
        return buildTeamResponse(savedTeam);
    }

    private void ensureCaptain(Long userId, Long teamId) {
        teamMemberRepository.findByTeamIdAndUserIdAndMemberRole(teamId, userId, CAPTAIN)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "仅队长可执行该操作"));
    }

    private TeamEntity mustFindTeam(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "队伍不存在"));
    }

    private UserAccountEntity mustFindUser(Long userId) {
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户不存在"));
    }

    private TeamInviteResponse toInviteResponse(TeamInviteEntity invite) {
        TeamEntity team = teamRepository.findById(invite.getTeamId()).orElse(null);
        UserAccountEntity inviter = userAccountRepository.findById(invite.getInviterId()).orElse(null);
        return new TeamInviteResponse(
                invite.getId(),
                invite.getTeamId(),
                team == null ? ("Team-" + invite.getTeamId()) : team.getName(),
                inviter == null ? ("User-" + invite.getInviterId()) : inviter.getRealName(),
                invite.getStatus(),
                invite.getCreatedAt().format(DATETIME_OUTPUT)
        );
    }

    private TeamResponse buildTeamResponse(TeamEntity team) {
        List<TeamMemberResponse> members = teamMemberRepository.findByTeamIdOrderByIdAsc(team.getId())
                .stream()
                .map(member -> {
                    UserAccountEntity account = mustFindUser(member.getUserId());
                    return new TeamMemberResponse(
                            account.getId(),
                            account.getUsername(),
                            account.getRealName(),
                            member.getMemberRole()
                    );
                })
                .toList();

        String coachName = null;
        if (team.getCoachId() != null) {
            coachName = userAccountRepository.findById(team.getCoachId())
                    .map(UserAccountEntity::getRealName)
                    .orElse("未知教练");
        }

        return new TeamResponse(team.getId(), team.getName(), team.getCoachId(), coachName, members);
    }

    private void sendInviteEmailIfEnabled(
            TeamEntity team,
            UserAccountEntity inviter,
            UserAccountEntity invitee,
            TeamInviteEntity invite
    ) {
        if (!inviteMailEnabled) {
            return;
        }
        String email = invitee.getEmail();
        if (email == null || email.isBlank()) {
            return;
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            logger.warn("Team invite mail is enabled but JavaMailSender is unavailable.");
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(inviteMailFrom);
        message.setTo(email.trim());
        message.setSubject("[ACM Train] 组队邀请：" + team.getName());
        message.setText(buildInviteMailBody(team, inviter, invitee, invite));
        try {
            mailSender.send(message);
        } catch (Exception ex) {
            logger.warn("Failed to send team invite mail, inviteId={}, inviteeId={}", invite.getId(), invitee.getId(), ex);
        }
    }

    private String buildInviteMailBody(
            TeamEntity team,
            UserAccountEntity inviter,
            UserAccountEntity invitee,
            TeamInviteEntity invite
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append(invitee.getRealName()).append(" 你好，\n\n");
        builder.append("你收到一条新的组队邀请，请及时登录系统查看。\n\n");
        builder.append("队伍名称：").append(team.getName()).append('\n');
        builder.append("邀请人：").append(inviter.getRealName()).append("（").append(inviter.getUsername()).append("）\n");
        builder.append("邀请时间：").append(invite.getCreatedAt().format(DATETIME_OUTPUT)).append('\n');
        builder.append('\n');
        builder.append("请登录 ACM Train 接受或拒绝该邀请。");
        return builder.toString();
    }
}
