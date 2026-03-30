package com.acmtrain.backend.repository;

import com.acmtrain.backend.entity.ContestLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ContestLinkRepository extends JpaRepository<ContestLinkEntity, Long> {
    List<ContestLinkEntity> findAllByOrderByStartTimeAscIdAsc();
    List<ContestLinkEntity> findAllBySourceTypeOrderByStartTimeAscIdAsc(String sourceType);
    List<ContestLinkEntity> findAllByRemindedAtIsNullAndStartTimeAfterOrderByStartTimeAscIdAsc(LocalDateTime startTime);
    Optional<ContestLinkEntity> findBySourceKey(String sourceKey);
}
