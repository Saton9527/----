package com.acmtrain.backend.repository;

import com.acmtrain.backend.entity.AlertLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertLogRepository extends JpaRepository<AlertLogEntity, Long> {
    List<AlertLogEntity> findAllByOrderByIdAsc();
}
