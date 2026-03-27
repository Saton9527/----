package com.acmtrain.backend.repository;

import com.acmtrain.backend.entity.ProblemsetLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProblemsetLinkRepository extends JpaRepository<ProblemsetLinkEntity, Long> {
    List<ProblemsetLinkEntity> findAllByOrderByIdDesc();
}
