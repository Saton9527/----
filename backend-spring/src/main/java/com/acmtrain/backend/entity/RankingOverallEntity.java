package com.acmtrain.backend.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "ranking_overall")
public class RankingOverallEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "rank_no", nullable = false)
    private Integer rankNo;
    @Column(name = "user_name", nullable = false)
    private String userName;
    @Column(name = "cf_rating", nullable = false)
    private Integer cfRating;
    @Column(name = "atc_rating", nullable = false)
    private Integer atcRating;
    @Column(name = "total_points", nullable = false)
    private BigDecimal totalPoints;
    @Column(name = "solved_count", nullable = false)
    private Integer solvedCount;
    @Column(name = "streak_days", nullable = false)
    private Integer streakDays;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getRankNo() { return rankNo; }
    public void setRankNo(Integer rankNo) { this.rankNo = rankNo; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public Integer getCfRating() { return cfRating; }
    public void setCfRating(Integer cfRating) { this.cfRating = cfRating; }
    public Integer getAtcRating() { return atcRating; }
    public void setAtcRating(Integer atcRating) { this.atcRating = atcRating; }
    public BigDecimal getTotalPoints() { return totalPoints; }
    public void setTotalPoints(BigDecimal totalPoints) { this.totalPoints = totalPoints; }
    public Integer getSolvedCount() { return solvedCount; }
    public void setSolvedCount(Integer solvedCount) { this.solvedCount = solvedCount; }
    public Integer getStreakDays() { return streakDays; }
    public void setStreakDays(Integer streakDays) { this.streakDays = streakDays; }
}
