package com.acmtrain.backend.repository;

import com.acmtrain.backend.entity.CoachTaskAssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CoachTaskAssignmentRepository extends JpaRepository<CoachTaskAssignmentEntity, Long> {
    List<CoachTaskAssignmentEntity> findByUserIdOrderByIdDesc(Long userId);
    List<CoachTaskAssignmentEntity> findByTaskIdOrderByIdAsc(Long taskId);
    Optional<CoachTaskAssignmentEntity> findByIdAndUserId(Long id, Long userId);
}