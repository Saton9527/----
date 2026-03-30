package com.acmtrain.backend.repository;

import com.acmtrain.backend.entity.PointLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PointLogRepository extends JpaRepository<PointLogEntity, Long> {
    List<PointLogEntity> findAllByOrderByIdAsc();
    Page<PointLogEntity> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);
    boolean existsBySourceKey(String sourceKey);
    void deleteByUserIdAndSourceType(Long userId, String sourceType);
    void deleteByUserIdAndSourceTypeAndSourceKeyStartingWith(Long userId, String sourceType, String sourceKeyPrefix);
    @Query("select coalesce(sum(p.points), 0) from PointLogEntity p where p.userId = :userId")
    BigDecimal sumPointsByUserId(@Param("userId") Long userId);
}
