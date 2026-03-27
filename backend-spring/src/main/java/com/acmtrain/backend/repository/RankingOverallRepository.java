package com.acmtrain.backend.repository;

import com.acmtrain.backend.entity.RankingOverallEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RankingOverallRepository extends JpaRepository<RankingOverallEntity, Long> {
    List<RankingOverallEntity> findAllByOrderByRankNoAsc();
}
