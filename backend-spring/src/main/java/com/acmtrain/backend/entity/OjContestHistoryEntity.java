package com.acmtrain.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "oj_contest_history")
public class OjContestHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(nullable = false)
    private String platform;
    @Column(name = "contest_name", nullable = false)
    private String contestName;
    @Column(name = "contest_url", nullable = false)
    private String contestUrl;
    @Column(name = "contest_time", nullable = false)
    private LocalDateTime contestTime;
    @Column(name = "rank_no")
    private Integer rankNo;
    @Column
    private Integer performance;
    @Column(name = "new_rating")
    private Integer newRating;
    @Column(name = "rating_change")
    private Integer ratingChange;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getContestName() { return contestName; }
    public void setContestName(String contestName) { this.contestName = contestName; }
    public String getContestUrl() { return contestUrl; }
    public void setContestUrl(String contestUrl) { this.contestUrl = contestUrl; }
    public LocalDateTime getContestTime() { return contestTime; }
    public void setContestTime(LocalDateTime contestTime) { this.contestTime = contestTime; }
    public Integer getRankNo() { return rankNo; }
    public void setRankNo(Integer rankNo) { this.rankNo = rankNo; }
    public Integer getPerformance() { return performance; }
    public void setPerformance(Integer performance) { this.performance = performance; }
    public Integer getNewRating() { return newRating; }
    public void setNewRating(Integer newRating) { this.newRating = newRating; }
    public Integer getRatingChange() { return ratingChange; }
    public void setRatingChange(Integer ratingChange) { this.ratingChange = ratingChange; }
}
