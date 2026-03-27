package com.acmtrain.backend.repository;

import com.acmtrain.backend.entity.TrendPointEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrendPointRepository extends JpaRepository<TrendPointEntity, Long> {
    List<TrendPointEntity> findAllByOrderByIdAsc();
}
