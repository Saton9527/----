package com.acmtrain.backend.repository;

import com.acmtrain.backend.entity.AlertLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface AlertLogRepository extends JpaRepository<AlertLogEntity, Long> {
    List<AlertLogEntity> findAllByOrderByIdAsc();
    Optional<AlertLogEntity> findTop1ByUserNameAndRuleCodeOrderByHitTimeDesc(String userName, String ruleCode);
    List<AlertLogEntity> findAllByStatusAndNotifiedAtIsNullOrderByHitTimeAsc(String status);
}
