package com.acmtrain.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "problemset_progress")
public class ProblemsetProgressEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "problemset_id", nullable = false)
    private Long problemsetId;

    @Column(nullable = false)
    private Boolean solved;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getProblemsetId() { return problemsetId; }
    public void setProblemsetId(Long problemsetId) { this.problemsetId = problemsetId; }
    public Boolean getSolved() { return solved; }
    public void setSolved(Boolean solved) { this.solved = solved; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}