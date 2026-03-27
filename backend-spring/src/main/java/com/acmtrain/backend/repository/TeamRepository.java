package com.acmtrain.backend.repository;

import com.acmtrain.backend.entity.TeamEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamRepository extends JpaRepository<TeamEntity, Long> {
    boolean existsByName(String name);
    List<TeamEntity> findByCoachIdOrderByIdDesc(Long coachId);
}