package com.acmtrain.backend.repository;

import com.acmtrain.backend.entity.TeamMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamMemberRepository extends JpaRepository<TeamMemberEntity, Long> {
    List<TeamMemberEntity> findByTeamIdOrderByIdAsc(Long teamId);
    long countByTeamId(Long teamId);
    Optional<TeamMemberEntity> findByUserId(Long userId);
    Optional<TeamMemberEntity> findByTeamIdAndUserId(Long teamId, Long userId);
    Optional<TeamMemberEntity> findByTeamIdAndUserIdAndMemberRole(Long teamId, Long userId, String memberRole);
}