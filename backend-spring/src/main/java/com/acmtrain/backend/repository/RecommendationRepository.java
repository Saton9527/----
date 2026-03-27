package com.acmtrain.backend.repository;

import com.acmtrain.backend.entity.RecommendationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecommendationRepository extends JpaRepository<RecommendationEntity, Long> {
    List<RecommendationEntity> findAllByOrderByIdAsc();
}
