package com.acmtrain.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "alert_log")
public class AlertLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
}
