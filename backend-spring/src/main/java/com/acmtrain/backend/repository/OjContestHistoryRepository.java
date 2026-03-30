package com.acmtrain.backend.repository;

import com.acmtrain.backend.entity.OjContestHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OjContestHistoryRepository extends JpaRepository<OjContestHistoryEntity, Long> {
    List<OjContestHistoryEntity> findTop20ByUserIdOrderByContestTimeDesc(Long userId);
    void deleteByUserId(Long userId);
}
