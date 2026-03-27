package com.acmtrain.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "student_info")
public class StudentInfoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;
    @Column(name = "real_name", nullable = false)
    private String realName;
    @Column(nullable = false)
    private String grade;
    @Column(nullable = false)
    private String major;
    @Column(name = "cf_handle", nullable = false)
    private String cfHandle;
    @Column(name = "atc_handle")
    private String atcHandle;
    @Column(name = "cf_rating", nullable = false)
    private Integer cfRating;
    @Column(name = "atc_rating", nullable = false)
    private Integer atcRating;
    @Column(name = "solved_count", nullable = false)
    private Integer solvedCount;
    @Column(name = "total_points", nullable = false)
    private Integer totalPoints;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public String getMajor() { return major; }
    public void setMajor(String major) { this.major = major; }
    public String getCfHandle() { return cfHandle; }
    public void setCfHandle(String cfHandle) { this.cfHandle = cfHandle; }
    public String getAtcHandle() { return atcHandle; }
    public void setAtcHandle(String atcHandle) { this.atcHandle = atcHandle; }
    public Integer getCfRating() { return cfRating; }
    public void setCfRating(Integer cfRating) { this.cfRating = cfRating; }
    public Integer getAtcRating() { return atcRating; }
    public void setAtcRating(Integer atcRating) { this.atcRating = atcRating; }
    public Integer getSolvedCount() { return solvedCount; }
    public void setSolvedCount(Integer solvedCount) { this.solvedCount = solvedCount; }
    public Integer getTotalPoints() { return totalPoints; }
    public void setTotalPoints(Integer totalPoints) { this.totalPoints = totalPoints; }
}
