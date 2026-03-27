package com.acmtrain.backend.repository;

import com.acmtrain.backend.entity.ContestLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContestLinkRepository extends JpaRepository<ContestLinkEntity, Long> {
    List<ContestLinkEntity> findAllByOrderByIdDesc();
}
