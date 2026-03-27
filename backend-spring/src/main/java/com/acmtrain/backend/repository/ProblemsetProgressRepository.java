package com.acmtrain.backend.repository;

import com.acmtrain.backend.entity.ProblemsetProgressEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProblemsetProgressRepository extends JpaRepository<ProblemsetProgressEntity, Long> {
    Optional<ProblemsetProgressEntity> findByUserIdAndProblemsetId(Long userId, Long problemsetId);
    List<ProblemsetProgressEntity> findAllByUserId(Long userId);
}