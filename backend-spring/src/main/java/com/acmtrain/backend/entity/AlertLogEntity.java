package com.acmtrain.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "alert_log")
public class AlertLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "user_name", nullable = false)
    private String userName;
    @Column(name = "rule_code", nullable = false)
    private String ruleCode;
    @Column(name = "risk_level", nullable = false)
    private String riskLevel;
    @Column(name = "hit_time", nullable = false)
    private LocalDateTime hitTime;
    @Column(nullable = false)
    private String status;
    @Column(nullable = false, length = 500)
    private String description;
    @Column(name = "suspicious_problems", length = 500)
    private String suspiciousProblems;
    @Column(length = 500)
    private String suggestion;
    @Column(name = "student_feedback", length = 500)
    private String studentFeedback;
    @Column(name = "feedback_at")
    private LocalDateTime feedbackAt;
    @Column(name = "notified_at")
    private LocalDateTime notifiedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getRuleCode() { return ruleCode; }
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public LocalDateTime getHitTime() { return hitTime; }
    public void setHitTime(LocalDateTime hitTime) { this.hitTime = hitTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSuspiciousProblems() { return suspiciousProblems; }
    public void setSuspiciousProblems(String suspiciousProblems) { this.suspiciousProblems = suspiciousProblems; }
    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    public String getStudentFeedback() { return studentFeedback; }
    public void setStudentFeedback(String studentFeedback) { this.studentFeedback = studentFeedback; }
    public LocalDateTime getFeedbackAt() { return feedbackAt; }
    public void setFeedbackAt(LocalDateTime feedbackAt) { this.feedbackAt = feedbackAt; }
    public LocalDateTime getNotifiedAt() { return notifiedAt; }
    public void setNotifiedAt(LocalDateTime notifiedAt) { this.notifiedAt = notifiedAt; }
}
