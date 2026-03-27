package com.acmtrain.backend.repository;

import com.acmtrain.backend.entity.PointLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointLogRepository extends JpaRepository<PointLogEntity, Long> {
    List<PointLogEntity> findAllByOrderByIdAsc();
}
