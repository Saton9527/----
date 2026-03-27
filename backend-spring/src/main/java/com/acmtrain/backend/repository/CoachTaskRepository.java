package com.acmtrain.backend.repository;

import com.acmtrain.backend.entity.CoachTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CoachTaskRepository extends JpaRepository<CoachTaskEntity, Long> {
    List<CoachTaskEntity> findByCoachIdOrderByIdDesc(Long coachId);
    List<CoachTaskEntity> findByCoachIdAndTeamIdOrderByIdDesc(Long coachId, Long teamId);
    List<CoachTaskEntity> findByTeamIdOrderByIdDesc(Long teamId);
}