package com.acmtrain.backend.repository;

import com.acmtrain.backend.entity.StudentInfoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentInfoRepository extends JpaRepository<StudentInfoEntity, Long> {
    List<StudentInfoEntity> findAllByOrderByIdAsc();
    Optional<StudentInfoEntity> findByUserId(Long userId);
}
