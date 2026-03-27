package com.acmtrain.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "training_task")
public class TrainingTaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String title;
    @Column(nullable = false)
    private String description;
    @Column(nullable = false)
    private LocalDateTime deadline;
    @Column(nullable = false)
    private String status;
    @Column(name = "total_problems", nullable = false)
    private Integer totalProblems;
    @Column(name = "completed_problems", nullable = false)
    private Integer completedProblems;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getTotalProblems() { return totalProblems; }
    public void setTotalProblems(Integer totalProblems) { this.totalProblems = totalProblems; }
    public Integer getCompletedProblems() { return completedProblems; }
    public void setCompletedProblems(Integer completedProblems) { this.completedProblems = completedProblems; }
}
