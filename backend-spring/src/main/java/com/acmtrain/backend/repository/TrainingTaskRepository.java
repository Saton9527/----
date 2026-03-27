package com.acmtrain.backend.repository;

import com.acmtrain.backend.entity.TrainingTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrainingTaskRepository extends JpaRepository<TrainingTaskEntity, Long> {
    List<TrainingTaskEntity> findAllByOrderByIdAsc();
    List<TrainingTaskEntity> findByStatusOrderByIdAsc(String status);
    org.springframework.data.domain.Page<TrainingTaskEntity> findByStatus(String status, org.springframework.data.domain.Pageable pageable);
}
