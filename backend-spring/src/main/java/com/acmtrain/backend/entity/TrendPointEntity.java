package com.acmtrain.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "trend_point")
public class TrendPointEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;
    @Column(nullable = false)
    private Integer solved;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getStatDate() { return statDate; }
    public void setStatDate(LocalDate statDate) { this.statDate = statDate; }
    public Integer getSolved() { return solved; }
    public void setSolved(Integer solved) { this.solved = solved; }
}
