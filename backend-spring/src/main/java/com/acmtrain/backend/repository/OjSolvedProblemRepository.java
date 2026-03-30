package com.acmtrain.backend.repository;

import com.acmtrain.backend.entity.OjSolvedProblemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OjSolvedProblemRepository extends JpaRepository<OjSolvedProblemEntity, Long> {
    void deleteByUserIdAndPlatform(Long userId, String platform);
    List<OjSolvedProblemEntity> findAllByUserIdOrderByAcceptedAtDesc(Long userId);
    List<OjSolvedProblemEntity> findTop12ByUserIdOrderByAcceptedAtDesc(Long userId);
    List<OjSolvedProblemEntity> findAllByUserIdAndAcceptedAtBetweenOrderByAcceptedAtAsc(Long userId, LocalDateTime start, LocalDateTime end);
    List<OjSolvedProblemEntity> findAllByAcceptedAtBetweenOrderByAcceptedAtAsc(LocalDateTime start, LocalDateTime end);
    long countByUserId(Long userId);
}
