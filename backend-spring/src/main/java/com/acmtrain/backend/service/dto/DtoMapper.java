package com.acmtrain.backend.service.dto;

import com.acmtrain.backend.dto.*;
import com.acmtrain.backend.entity.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
                entity.getUserId(),
                entity.getUserName(),
                entity.getRuleCode(),
                entity.getRiskLevel(),
                entity.getHitTime().format(DATETIME_OUTPUT),
                entity.getStatus(),
                entity.getDescription(),
                entity.getSuspiciousProblems(),
                entity.getSuggestion(),
                entity.getNotifiedAt() == null ? null : entity.getNotifiedAt().format(DATETIME_OUTPUT),
                entity.getStudentFeedback(),
                entity.getFeedbackAt() == null ? null : entity.getFeedbackAt().format(DATETIME_OUTPUT)
        );
    }

    public static StudentResponse toStudentResponse(StudentInfoEntity entity) {
        return new StudentResponse(
                entity.getId(),
                entity.getUserId(),
                null,
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
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = entity.getStartTime();
        LocalDateTime reminderTime = startTime.minusMinutes(entity.getReminderMinutes());
        String status;
        if (startTime.isBefore(now)) {
            status = "FINISHED";
        } else if (startTime.toLocalDate().isEqual(now.toLocalDate())) {
            status = "TODAY";
        } else {
            status = "UPCOMING";
        }

        return new ContestResponse(
                entity.getId(),
                entity.getPlatform(),
                entity.getSourceType(),
                entity.getTitle(),
                entity.getUrl(),
                startTime.format(DATETIME_OUTPUT),
                reminderTime.format(DATETIME_OUTPUT),
                entity.getReminderMinutes(),
                status
        );
    }

    public static CoachTaskResponse toCoachTaskResponse(CoachTaskEntity entity) {
        return new CoachTaskResponse(
                entity.getId(),
                entity.getTeamId(),
                null,
                entity.getTitle(),
                entity.getDescription(),
                entity.getDeadline().format(DATETIME_OUTPUT),
                entity.getCreatedAt().format(DATETIME_OUTPUT),
                0,
                0,
                0,
                List.of()
        );
    }
}
