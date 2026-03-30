package com.acmtrain.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "oj_solved_problem")
public class OjSolvedProblemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(nullable = false)
    private String platform;
    @Column(name = "problem_code", nullable = false)
    private String problemCode;
    @Column(nullable = false)
    private String title;
    @Column(name = "problem_url", nullable = false)
    private String problemUrl;
    @Column
    private Integer rating;
    @Column
    private String tag;
    @Column(name = "accepted_at", nullable = false)
    private LocalDateTime acceptedAt;
    @Column(name = "source_key", nullable = false, unique = true)
    private String sourceKey;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getProblemCode() { return problemCode; }
    public void setProblemCode(String problemCode) { this.problemCode = problemCode; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getProblemUrl() { return problemUrl; }
    public void setProblemUrl(String problemUrl) { this.problemUrl = problemUrl; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
    public LocalDateTime getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(LocalDateTime acceptedAt) { this.acceptedAt = acceptedAt; }
    public String getSourceKey() { return sourceKey; }
    public void setSourceKey(String sourceKey) { this.sourceKey = sourceKey; }
}
