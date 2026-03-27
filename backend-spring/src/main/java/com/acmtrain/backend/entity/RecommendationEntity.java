package com.acmtrain.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "recommendation")
public class RecommendationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String level;
    @Column(name = "problem_code", nullable = false)
    private String problemCode;
    @Column(nullable = false)
    private String title;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getProblemCode() { return problemCode; }
    public void setProblemCode(String problemCode) { this.problemCode = problemCode; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
}
