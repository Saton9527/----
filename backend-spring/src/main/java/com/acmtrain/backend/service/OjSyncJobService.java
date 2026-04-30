package com.acmtrain.backend.service;

import com.acmtrain.backend.dto.MyProfileResponse;
import com.acmtrain.backend.dto.MyProfileSyncJobResponse;
import com.acmtrain.backend.dto.StudentResponse;
import com.acmtrain.backend.dto.StudentSyncJobResponse;
import jakarta.annotation.PreDestroy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class OjSyncJobService {

    private static final DateTimeFormatter DATETIME_OUTPUT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final long RETENTION_HOURS = 6;

    private final OjSyncService ojSyncService;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Map<String, JobState> jobs = new ConcurrentHashMap<>();
    private final Map<String, String> runningJobKeys = new ConcurrentHashMap<>();

    public OjSyncJobService(OjSyncService ojSyncService) {
        this.ojSyncService = ojSyncService;
    }

    public MyProfileSyncJobResponse startMyProfileSync(Long userId) {
        cleanupFinishedJobs();
        String key = "my:" + userId;
        JobState existing = findRunningJob(key);
        if (existing != null) {
            return toMyProfileJobResponse(existing);
        }

        JobState job = new JobState(
                UUID.randomUUID().toString(),
                "MY_PROFILE",
                userId,
                null
        );
        jobs.put(job.jobId, job);
        runningJobKeys.put(key, job.jobId);
        executor.submit(() -> runMyProfileJob(job, key));
        return toMyProfileJobResponse(job);
    }

    public MyProfileSyncJobResponse getMyProfileSync(Long userId, String jobId) {
        JobState job = getJob(jobId);
        if (!"MY_PROFILE".equals(job.kind) || !userId.equals(job.ownerUserId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "同步任务不存在");
        }
        return toMyProfileJobResponse(job);
    }

    public StudentSyncJobResponse startStudentSync(Long operatorId, Long studentId) {
        cleanupFinishedJobs();
        String key = "student:" + operatorId + ":" + studentId;
        JobState existing = findRunningJob(key);
        if (existing != null) {
            return toStudentJobResponse(existing);
        }

        JobState job = new JobState(
                UUID.randomUUID().toString(),
                "STUDENT",
                operatorId,
                studentId
        );
        jobs.put(job.jobId, job);
        runningJobKeys.put(key, job.jobId);
        executor.submit(() -> runStudentJob(job, key));
        return toStudentJobResponse(job);
    }

    public StudentSyncJobResponse getStudentSync(Long operatorId, Long studentId, String jobId) {
        JobState job = getJob(jobId);
        if (!"STUDENT".equals(job.kind)
                || !operatorId.equals(job.ownerUserId)
                || !studentId.equals(job.studentId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "同步任务不存在");
        }
        return toStudentJobResponse(job);
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }

    private void runMyProfileJob(JobState job, String key) {
        job.status = "RUNNING";
        job.message = "正在同步真实 OJ 数据";
        try {
            MyProfileResponse profile = ojSyncService.syncMyProfile(job.ownerUserId);
            job.profile = profile;
            job.status = "SUCCESS";
            job.message = "真实 OJ 数据同步完成";
        } catch (ResponseStatusException ex) {
            job.status = "FAILED";
            job.message = ex.getReason() == null || ex.getReason().isBlank() ? "同步失败" : ex.getReason();
        } catch (Exception ex) {
            job.status = "FAILED";
            job.message = "同步失败，请稍后重试";
        } finally {
            job.finishedAt = LocalDateTime.now();
            runningJobKeys.remove(key);
        }
    }

    private void runStudentJob(JobState job, String key) {
        job.status = "RUNNING";
        job.message = "正在同步学生真实 OJ 数据";
        try {
            StudentResponse student = ojSyncService.syncStudentById(job.ownerUserId, job.studentId);
            job.student = student;
            job.status = "SUCCESS";
            job.message = "学生真实 OJ 数据同步完成";
        } catch (ResponseStatusException ex) {
            job.status = "FAILED";
            job.message = ex.getReason() == null || ex.getReason().isBlank() ? "同步失败" : ex.getReason();
        } catch (Exception ex) {
            job.status = "FAILED";
            job.message = "同步失败，请稍后重试";
        } finally {
            job.finishedAt = LocalDateTime.now();
            runningJobKeys.remove(key);
        }
    }

    private JobState findRunningJob(String key) {
        String jobId = runningJobKeys.get(key);
        if (jobId == null) {
            return null;
        }
        JobState job = jobs.get(jobId);
        if (job == null || !"PENDING".equals(job.status) && !"RUNNING".equals(job.status)) {
            runningJobKeys.remove(key);
            return null;
        }
        return job;
    }

    private JobState getJob(String jobId) {
        JobState job = jobs.get(jobId);
        if (job == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "同步任务不存在");
        }
        return job;
    }

    private void cleanupFinishedJobs() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(RETENTION_HOURS);
        jobs.values().removeIf(job -> job.finishedAt != null && job.finishedAt.isBefore(cutoff));
    }

    private MyProfileSyncJobResponse toMyProfileJobResponse(JobState job) {
        return new MyProfileSyncJobResponse(
                job.jobId,
                job.status,
                job.message,
                formatDateTime(job.startedAt),
                formatDateTime(job.finishedAt),
                job.profile
        );
    }

    private StudentSyncJobResponse toStudentJobResponse(JobState job) {
        return new StudentSyncJobResponse(
                job.jobId,
                job.studentId,
                job.status,
                job.message,
                formatDateTime(job.startedAt),
                formatDateTime(job.finishedAt),
                job.student
        );
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.format(DATETIME_OUTPUT);
    }

    private static final class JobState {
        private final String jobId;
        private final String kind;
        private final Long ownerUserId;
        private final Long studentId;
        private final LocalDateTime startedAt;
        private volatile LocalDateTime finishedAt;
        private volatile String status;
        private volatile String message;
        private volatile MyProfileResponse profile;
        private volatile StudentResponse student;

        private JobState(String jobId, String kind, Long ownerUserId, Long studentId) {
            this.jobId = jobId;
            this.kind = kind;
            this.ownerUserId = ownerUserId;
            this.studentId = studentId;
            this.startedAt = LocalDateTime.now();
            this.status = "PENDING";
            this.message = "同步任务已创建";
        }
    }
}
