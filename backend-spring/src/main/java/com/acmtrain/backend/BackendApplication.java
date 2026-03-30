package com.acmtrain.backend;

import com.acmtrain.backend.entity.RankingOverallEntity;
import com.acmtrain.backend.entity.StudentInfoEntity;
import com.acmtrain.backend.entity.TrendPointEntity;
import com.acmtrain.backend.entity.UserAccountEntity;
import com.acmtrain.backend.repository.RankingOverallRepository;
import com.acmtrain.backend.repository.StudentInfoRepository;
import com.acmtrain.backend.repository.TrendPointRepository;
import com.acmtrain.backend.repository.UserAccountRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@SpringBootApplication
@EnableCaching
@EnableScheduling
public class BackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

    @Bean
    public org.springframework.boot.CommandLineRunner seedDemoData(
            UserAccountRepository userAccountRepository,
            StudentInfoRepository studentInfoRepository,
            RankingOverallRepository rankingOverallRepository,
            TrendPointRepository trendPointRepository
    ) {
        return args -> {
            if (userAccountRepository.count() > 0) {
                return;
            }

            UserAccountEntity coach01 = new UserAccountEntity();
            coach01.setUsername("coach01");
            coach01.setPassword("123456");
            coach01.setRealName("演示教练A");
            coach01.setRole("coach");

            UserAccountEntity student01 = new UserAccountEntity();
            student01.setUsername("student01");
            student01.setPassword("123456");
            student01.setRealName("演示学生A");
            student01.setRole("student");

            UserAccountEntity student02 = new UserAccountEntity();
            student02.setUsername("student02");
            student02.setPassword("123456");
            student02.setRealName("演示学生B");
            student02.setRole("student");

            UserAccountEntity student03 = new UserAccountEntity();
            student03.setUsername("student03");
            student03.setPassword("123456");
            student03.setRealName("演示学生C");
            student03.setRole("student");

            UserAccountEntity coach02 = new UserAccountEntity();
            coach02.setUsername("coach02");
            coach02.setPassword("123456");
            coach02.setRealName("演示教练B");
            coach02.setRole("coach");

            UserAccountEntity demoStudent = new UserAccountEntity();
            demoStudent.setUsername("5baf2d92");
            demoStudent.setPassword("123456");
            demoStudent.setRealName("演示用户");
            demoStudent.setRole("student");

            List<UserAccountEntity> users = userAccountRepository.saveAll(List.of(
                    coach01, student01, student02, student03, coach02, demoStudent
            ));

            studentInfoRepository.saveAll(List.of(
                    studentInfo(users.get(1).getId(), "演示学生A", "2023", "计算机科学与技术", null, null, 0, 0, 0, BigDecimal.valueOf(24.0)),
                    studentInfo(users.get(2).getId(), "演示学生B", "2023", "软件工程", "Benq", "Benq", 3792, 3658, 145, BigDecimal.valueOf(221.0)),
                    studentInfo(users.get(3).getId(), "演示学生C", "2024", "数据科学与大数据技术", "ecnerwala", "ecnerwala", 3696, 3619, 123, BigDecimal.valueOf(198.0)),
                    studentInfo(users.get(5).getId(), "演示用户", "2024", "人工智能", "rng_58", "rng_58", 3074, 0, 98, BigDecimal.valueOf(186.0))
            ));

            rankingOverallRepository.saveAll(List.of(
                    ranking(4, "演示学生A", 0, 0, BigDecimal.valueOf(24.0), 0, 1),
                    ranking(1, "演示学生B", 3792, 3658, BigDecimal.valueOf(221.0), 145, 7),
                    ranking(2, "演示学生C", 3696, 3619, BigDecimal.valueOf(198.0), 123, 5),
                    ranking(3, "演示用户", 3074, 0, BigDecimal.valueOf(186.0), 98, 6)
            ));

            trendPointRepository.saveAll(List.of(
                    trendPoint("2026-03-01", 3),
                    trendPoint("2026-03-02", 5),
                    trendPoint("2026-03-03", 2),
                    trendPoint("2026-03-04", 6),
                    trendPoint("2026-03-05", 4),
                    trendPoint("2026-03-06", 3),
                    trendPoint("2026-03-07", 4)
            ));
        };
    }

    private static StudentInfoEntity studentInfo(
            Long userId,
            String realName,
            String grade,
            String major,
            String cfHandle,
            String atcHandle,
            Integer cfRating,
            Integer atcRating,
            Integer solvedCount,
            BigDecimal totalPoints
    ) {
        StudentInfoEntity entity = new StudentInfoEntity();
        entity.setUserId(userId);
        entity.setRealName(realName);
        entity.setGrade(grade);
        entity.setMajor(major);
        entity.setCfHandle(cfHandle);
        entity.setAtcHandle(atcHandle);
        entity.setCfRating(cfRating);
        entity.setAtcRating(atcRating);
        entity.setSolvedCount(solvedCount);
        entity.setTotalPoints(totalPoints);
        return entity;
    }

    private static RankingOverallEntity ranking(
            Integer rankNo,
            String userName,
            Integer cfRating,
            Integer atcRating,
            BigDecimal totalPoints,
            Integer solvedCount,
            Integer streakDays
    ) {
        RankingOverallEntity entity = new RankingOverallEntity();
        entity.setRankNo(rankNo);
        entity.setUserName(userName);
        entity.setCfRating(cfRating);
        entity.setAtcRating(atcRating);
        entity.setTotalPoints(totalPoints);
        entity.setSolvedCount(solvedCount);
        entity.setStreakDays(streakDays);
        return entity;
    }

    private static TrendPointEntity trendPoint(String statDate, Integer solved) {
        TrendPointEntity entity = new TrendPointEntity();
        entity.setStatDate(LocalDate.parse(statDate));
        entity.setSolved(solved);
        return entity;
    }
}
