package com.acmtrain.backend.repository;

import com.acmtrain.backend.entity.TeamInviteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamInviteRepository extends JpaRepository<TeamInviteEntity, Long> {
    List<TeamInviteEntity> findByInviteeIdAndStatusOrderByIdDesc(Long inviteeId, String status);
    Optional<TeamInviteEntity> findByTeamIdAndInviteeIdAndStatus(Long teamId, Long inviteeId, String status);
    List<TeamInviteEntity> findByTeamIdAndStatusOrderByIdDesc(Long teamId, String status);
}