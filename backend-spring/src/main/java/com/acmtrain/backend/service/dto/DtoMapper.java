package com.acmtrain.backend.service.dto;

import com.acmtrain.backend.dto.*;
import com.acmtrain.backend.entity.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DtoMapper {

    private static final DateTimeFormatter DATETIME_OUTPUT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE_OUTPUT = DateTimeFormatter.ofPattern("MM-dd");

    public static TaskResponse toTaskResponse(TrainingTaskEntity entity) {
        return new TaskResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getDeadline().format(DATETIME_OUTPUT),
                entity.getStatus(),
                entity.getTotalProblems(),
                entity.getCompletedProblems()
        );
    }

    public static RankingResponse toRankingResponse(RankingOverallEntity entity) {
        return toRankingResponse(entity, entity.getRankNo());
    }

    public static RankingResponse toRankingResponse(RankingOverallEntity entity, Integer rankNo) {
        return new RankingResponse(
                rankNo,
                entity.getUserName(),
                entity.getCfRating(),
                entity.getAtcRating(),
                entity.getTotalPoints(),
                entity.getSolvedCount(),
                entity.getStreakDays()
        );
    }

    public static PointLogResponse toPointLogResponse(PointLogEntity entity) {
        return new PointLogResponse(
                entity.getId(),
                entity.getSourceType(),
                entity.getReason(),
                entity.getPoints(),
                entity.getCreatedAt().format(DATETIME_OUTPUT)
        );
    }

    public static TrendPointResponse toTrendPointResponse(TrendPointEntity entity) {
        return new TrendPointResponse(
                entity.getStatDate().format(DATE_OUTPUT),
                entity.getSolved()
        );
    }

    public static RecommendationResponse toRecommendationResponse(RecommendationEntity entity) {
        return new RecommendationResponse(
                entity.getId(),
                entity.getLevel(),
                entity.getProblemCode(),
                entity.getTitle(),
                null,
                null,
                "系统默认推荐"
        );
    }

    public static AlertResponse toAlertResponse(AlertLogEntity entity) {
        return new AlertResponse(
                entity.getId(),
                entity.getUserName(),
                entity.getRuleCode(),
                entity.getRiskLevel(),
                entity.getHitTime().format(DATETIME_OUTPUT),
                entity.getStatus()
        );
    }

    public static StudentResponse toStudentResponse(StudentInfoEntity entity) {
        return new StudentResponse(
                entity.getId(),
                entity.getUserId(),
                entity.getRealName(),
                entity.getGrade(),
                entity.getMajor(),
                entity.getCfHandle(),
                entity.getAtcHandle(),
                entity.getCfRating(),
                entity.getAtcRating(),
                entity.getSolvedCount(),
                entity.getTotalPoints()
        );
    }

    public static ProblemsetResponse toProblemsetResponse(ProblemsetLinkEntity entity) {
        return toProblemsetResponse(entity, false, null);
    }

    public static ProblemsetResponse toProblemsetResponse(ProblemsetLinkEntity entity, boolean solved, LocalDateTime solvedAt) {
        return new ProblemsetResponse(
                entity.getId(),
                entity.getPlatform(),
                entity.getTitle(),
                entity.getUrl(),
                solved,
                solvedAt == null ? null : solvedAt.format(DATETIME_OUTPUT)
        );
    }

    public static ContestResponse toContestResponse(ContestLinkEntity entity) {
        return new ContestResponse(
                entity.getId(),
                entity.getPlatform(),
                entity.getTitle(),
                entity.getUrl()
        );
    }

    public static CoachTaskResponse toCoachTaskResponse(CoachTaskEntity entity) {
        return new CoachTaskResponse(
                entity.getId(),
                entity.getTeamId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getDeadline().format(DATETIME_OUTPUT),
                entity.getCreatedAt().format(DATETIME_OUTPUT)
        );
    }
}