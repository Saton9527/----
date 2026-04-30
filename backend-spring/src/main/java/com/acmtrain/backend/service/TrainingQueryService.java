package com.acmtrain.backend.service;

import com.acmtrain.backend.dto.*;
import com.acmtrain.backend.entity.AlertLogEntity;
import com.acmtrain.backend.entity.OjSolvedProblemEntity;
import com.acmtrain.backend.entity.RankingOverallEntity;
import com.acmtrain.backend.entity.StudentInfoEntity;
import com.acmtrain.backend.entity.TrainingTaskEntity;
import com.acmtrain.backend.entity.UserAccountEntity;
import com.acmtrain.backend.repository.*;
import com.acmtrain.backend.service.dto.DtoMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

@Service
public class TrainingQueryService {

    private enum RankingMetric {
        TOTAL_POINTS,
        CF_RATING,
        ATC_RATING,
        SOLVED_COUNT
    }

    private record ProblemSeed(String code, String title, int rating, String tag, String platform) {
    }

    private record RecommendationPlan(String level, int targetOffset, String levelReason) {
    }

    private static final Logger logger = LoggerFactory.getLogger(TrainingQueryService.class);
    private static final DateTimeFormatter DATETIME_INPUT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE_OUTPUT = DateTimeFormatter.ofPattern("MM-dd");
    private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");
    private static final List<RecommendationPlan> RECOMMENDATION_PLANS = List.of(
            new RecommendationPlan("WARMUP", -220, "先用低压题把手感和正确率找回来。"),
            new RecommendationPlan("WARMUP", -120, "继续热身，优先补足基础题型。"),
            new RecommendationPlan("CORE", -20, "进入主训练区，先贴着当前水平稳定输出。"),
            new RecommendationPlan("CORE", 80, "主训练区稍抬一档，适合稳步提难度。"),
            new RecommendationPlan("CHALLENGE", 180, "开始进入挑战区，适合做一两道拉伸题。"),
            new RecommendationPlan("CHALLENGE", 300, "最后留一题更高压的冲刺题。")
    );

    private final TrainingTaskRepository trainingTaskRepository;
    private final RankingOverallRepository rankingOverallRepository;
    private final PointLogRepository pointLogRepository;
    private final TrendPointRepository trendPointRepository;
    private final RecommendationRepository recommendationRepository;
    private final AlertLogRepository alertLogRepository;
    private final StudentInfoRepository studentInfoRepository;
    private final UserAccountRepository userAccountRepository;
    private final OjSolvedProblemRepository ojSolvedProblemRepository;
    private final CodeforcesCatalogService codeforcesCatalogService;
    private final OjAccountValidationService ojAccountValidationService;

    public TrainingQueryService(
            TrainingTaskRepository trainingTaskRepository,
            RankingOverallRepository rankingOverallRepository,
            PointLogRepository pointLogRepository,
            TrendPointRepository trendPointRepository,
            RecommendationRepository recommendationRepository,
            AlertLogRepository alertLogRepository,
            StudentInfoRepository studentInfoRepository,
            UserAccountRepository userAccountRepository,
            OjSolvedProblemRepository ojSolvedProblemRepository,
            CodeforcesCatalogService codeforcesCatalogService,
            OjAccountValidationService ojAccountValidationService
    ) {
        this.trainingTaskRepository = trainingTaskRepository;
        this.rankingOverallRepository = rankingOverallRepository;
        this.pointLogRepository = pointLogRepository;
        this.trendPointRepository = trendPointRepository;
        this.recommendationRepository = recommendationRepository;
        this.alertLogRepository = alertLogRepository;
        this.studentInfoRepository = studentInfoRepository;
        this.userAccountRepository = userAccountRepository;
        this.ojSolvedProblemRepository = ojSolvedProblemRepository;
        this.codeforcesCatalogService = codeforcesCatalogService;
        this.ojAccountValidationService = ojAccountValidationService;
    }

    @Cacheable(value = "tasks", key = "#status + '_' + #page + '_' + #size")
    public PageResponse<TaskResponse> tasks(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<TrainingTaskEntity> entityPage;

        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            entityPage = trainingTaskRepository.findAll(pageable);
        } else {
            entityPage = trainingTaskRepository.findByStatus(status, pageable);
        }

        List<TaskResponse> content = entityPage.stream()
                .map(DtoMapper::toTaskResponse)
                .toList();

        return new PageResponse<>(
                content,
                entityPage.getNumber(),
                entityPage.getSize(),
                entityPage.getTotalElements(),
                entityPage.getTotalPages(),
                entityPage.isLast(),
                entityPage.isFirst()
        );
    }

    @Transactional
    @CacheEvict(value = "tasks", allEntries = true)
    public TaskResponse createTask(CreateTaskRequest request) {
        logger.info("Creating new task: {}", request.title());
        TrainingTaskEntity entity = new TrainingTaskEntity();
        entity.setTitle(request.title());
        entity.setDescription(request.description());
        entity.setDeadline(parseDateTime(request.deadline()));
        entity.setStatus(request.status() == null || request.status().isBlank() ? "PUBLISHED" : request.status());
        entity.setTotalProblems(request.totalProblems());
        entity.setCompletedProblems(0);
        TrainingTaskEntity savedEntity = trainingTaskRepository.save(entity);
        logger.info("Task created successfully with id: {}", savedEntity.getId());
        return DtoMapper.toTaskResponse(savedEntity);
    }

    @Transactional
    @CacheEvict(value = "tasks", allEntries = true)
    public TaskResponse updateTaskStatus(Long id, UpdateTaskStatusRequest request) {
        logger.info("Updating task status for id: {}, new status: {}", id, request.status());
        TrainingTaskEntity entity = trainingTaskRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "任务不存在"));
        entity.setStatus(request.status());
        TrainingTaskEntity updatedEntity = trainingTaskRepository.save(entity);
        logger.info("Task status updated successfully for id: {}", id);
        return DtoMapper.toTaskResponse(updatedEntity);
    }

    @Transactional
    @CacheEvict(value = "tasks", allEntries = true)
    public TaskResponse updateTaskProgress(Long id, UpdateTaskProgressRequest request) {
        logger.info("Updating task progress for id: {}, completed problems: {}", id, request.completedProblems());
        TrainingTaskEntity entity = trainingTaskRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "任务不存在"));

        int max = entity.getTotalProblems();
        int value = request.completedProblems() == null ? 0 : request.completedProblems();
        if (value < 0 || value > max) {
            logger.warn("Invalid completed problems value: {} for task id: {}, max: {}", value, id, max);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "completedProblems 超出范围");
        }

        entity.setCompletedProblems(value);
        if (value == max) {
            entity.setStatus("DONE");
            logger.info("Task {} marked as DONE", id);
        } else if ("DONE".equals(entity.getStatus())) {
            entity.setStatus("PUBLISHED");
            logger.info("Task {} marked as PUBLISHED", id);
        }

        TrainingTaskEntity updatedEntity = trainingTaskRepository.save(entity);
        logger.info("Task progress updated successfully for id: {}", id);
        return DtoMapper.toTaskResponse(updatedEntity);
    }

    @Cacheable(value = "rankings", key = "#metric + '_' + #page + '_' + #size")
    public PageResponse<RankingResponse> rankings(String metric, int page, int size) {
        validatePage(page, size);
        RankingMetric rankingMetric = resolveRankingMetric(metric);

        List<RankingOverallEntity> sorted = rankingOverallRepository.findAll().stream()
                .sorted(buildRankingComparator(rankingMetric))
                .toList();

        List<RankingResponse> allRows = IntStream.range(0, sorted.size())
                .mapToObj(i -> DtoMapper.toRankingResponse(sorted.get(i), i + 1))
                .toList();

        return slicePage(allRows, page, size);
    }

    @Cacheable(value = "points", key = "#userId + '_' + #page + '_' + #size")
    public PageResponse<PointLogResponse> points(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<?> entityPage = pointLogRepository.findByUserIdOrderByCreatedAtDescIdDesc(userId, pageable);

        List<PointLogResponse> content = entityPage.stream()
                .map(e -> DtoMapper.toPointLogResponse((com.acmtrain.backend.entity.PointLogEntity) e))
                .toList();

        return new PageResponse<>(
                content,
                entityPage.getNumber(),
                entityPage.getSize(),
                entityPage.getTotalElements(),
                entityPage.getTotalPages(),
                entityPage.isLast(),
                entityPage.isFirst()
        );
    }

    @Cacheable(value = "trend", key = "#userId")
    public List<TrendPointResponse> trend(Long userId) {
        UserAccountEntity user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));

        LocalDate today = LocalDate.now(SHANGHAI_ZONE);
        LocalDate startDate = today.minusDays(6);
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = today.plusDays(1).atStartOfDay();

        List<OjSolvedProblemEntity> solvedProblems = "coach".equalsIgnoreCase(user.getRole())
                ? ojSolvedProblemRepository.findAllByAcceptedAtBetweenOrderByAcceptedAtAsc(startTime, endTime)
                : ojSolvedProblemRepository.findAllByUserIdAndAcceptedAtBetweenOrderByAcceptedAtAsc(userId, startTime, endTime);

        Map<LocalDate, Integer> solvedByDate = solvedProblems.stream()
                .collect(java.util.stream.Collectors.toMap(
                        item -> item.getAcceptedAt().toLocalDate(),
                        item -> 1,
                        Integer::sum
                ));

        return startDate.datesUntil(today.plusDays(1))
                .map(date -> new TrendPointResponse(
                        date.format(DATE_OUTPUT),
                        solvedByDate.getOrDefault(date, 0)
                ))
                .toList();
    }

    public DashboardAnalyticsResponse dashboardAnalytics(Long userId) {
        StudentInfoEntity student = studentInfoRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "当前用户暂无学生档案"));
        List<OjSolvedProblemEntity> solvedProblems = ojSolvedProblemRepository.findAllByUserIdOrderByAcceptedAtDesc(userId);

        int totalSolved = solvedProblems.isEmpty()
                ? (student.getSolvedCount() == null ? 0 : student.getSolvedCount())
                : solvedProblems.size();
        int hiddenRating = computeHiddenRating(student);
        List<ProblemBucketResponse> buckets = solvedProblems.isEmpty()
                ? buildProblemBuckets(totalSolved, hiddenRating)
                : buildProblemBuckets(solvedProblems);
        List<ProblemTagResponse> tags = solvedProblems.isEmpty()
                ? buildProblemTags(totalSolved, hiddenRating)
                : buildProblemTags(solvedProblems);
        List<ProblemDetailResponse> recentSolved = solvedProblems.isEmpty()
                ? buildProblemDetails(hiddenRating, tags)
                : buildProblemDetails(solvedProblems);
        return new DashboardAnalyticsResponse(totalSolved, hiddenRating, buckets, tags, recentSolved);
    }

    @Cacheable(value = "myProfile", key = "#userId")
    public MyProfileResponse myProfile(Long userId) {
        UserAccountEntity user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
        StudentInfoEntity student = studentInfoRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "当前用户暂无学生档案"));
        return toMyProfileResponse(user, student);
    }

    @Transactional
    @CacheEvict(value = {"myProfile", "students", "recommendations"}, allEntries = true)
    public MyProfileResponse updatePlatformBinding(Long userId, UpdatePlatformBindingRequest request) {
        StudentInfoEntity student = studentInfoRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "当前用户暂无学生档案"));

        String previousCfHandle = normalizeOptional(student.getCfHandle());
        String previousAtcHandle = normalizeOptional(student.getAtcHandle());
        String cfHandle = normalizeOptional(request.cfHandle());
        String atcHandle = normalizeOptional(request.atcHandle());
        validateBoundHandles(cfHandle, atcHandle);

        student.setCfHandle(cfHandle);
        student.setAtcHandle(atcHandle);
        if (!equalsIgnoreCase(previousCfHandle, cfHandle)) {
            ojSolvedProblemRepository.deleteByUserIdAndPlatform(userId, "Codeforces");
            student.setCfRating(0);
            student.setCfSyncedHandle(null);
            student.setCfLastSubmissionEpochSecond(null);
        }
        if (!equalsIgnoreCase(previousAtcHandle, atcHandle)) {
            ojSolvedProblemRepository.deleteByUserIdAndPlatform(userId, "AtCoder");
            student.setAtcRating(0);
            student.setAtcSyncedHandle(null);
            student.setAtcLastSubmissionEpochSecond(null);
        }
        if (!equalsIgnoreCase(previousCfHandle, cfHandle) || !equalsIgnoreCase(previousAtcHandle, atcHandle)) {
            student.setSolvedCount(0);
            student.setTotalPoints(BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP));
        }
        StudentInfoEntity updated = studentInfoRepository.save(student);

        UserAccountEntity user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
        return toMyProfileResponse(user, updated);
    }

    @Cacheable(value = "recommendations", key = "#userId + '_' + #page + '_' + #size")
    public PageResponse<RecommendationResponse> recommendations(Long userId, int page, int size) {
        validatePage(page, size);
        StudentInfoEntity student = studentInfoRepository.findByUserId(userId).orElse(null);
        if (student == null) {
            Pageable pageable = PageRequest.of(page, size);
            Page<?> entityPage = recommendationRepository.findAll(pageable);

            List<RecommendationResponse> content = entityPage.stream()
                    .map(e -> DtoMapper.toRecommendationResponse((com.acmtrain.backend.entity.RecommendationEntity) e))
                    .toList();

            return new PageResponse<>(
                    content,
                    entityPage.getNumber(),
                    entityPage.getSize(),
                    entityPage.getTotalElements(),
                    entityPage.getTotalPages(),
                    entityPage.isLast(),
                    entityPage.isFirst()
            );
        }

        List<RecommendationResponse> all = buildAlgorithmRecommendations(student);
        return slicePage(all, page, size);
    }

    @Cacheable(value = "problems", key = "#userId + '_' + #keyword + '_' + #minRating + '_' + #maxRating + '_' + #solved + '_' + #recommended + '_' + #page + '_' + #size")
    public PageResponse<ProblemCatalogResponse> problems(
            Long userId,
            String keyword,
            Integer minRating,
            Integer maxRating,
            Boolean solved,
            Boolean recommended,
            int page,
            int size
    ) {
        validatePage(page, size);
        if (minRating != null && maxRating != null && minRating > maxRating) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minRating 不能大于 maxRating");
        }

        StudentInfoEntity student = studentInfoRepository.findByUserId(userId).orElse(null);
        int hiddenRating = student == null ? 0 : computeHiddenRating(student);
        List<OjSolvedProblemEntity> solvedProblems = ojSolvedProblemRepository.findAllByUserIdOrderByAcceptedAtDesc(userId);
        boolean hasRealSolvedData = !solvedProblems.isEmpty();
        Set<String> solvedCodes = solvedProblems.stream()
                .map(OjSolvedProblemEntity::getProblemCode)
                .collect(java.util.stream.Collectors.toSet());
        Set<String> recommendedCodes = student == null
                ? Set.of()
                : buildAlgorithmRecommendations(student).stream()
                .map(RecommendationResponse::problemCode)
                .collect(java.util.stream.Collectors.toSet());
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);

        List<ProblemCatalogResponse> rows = mergeProblemCatalog(student, hiddenRating, solvedProblems, hasRealSolvedData, solvedCodes, recommendedCodes).stream()
                .filter(item -> normalizedKeyword.isBlank()
                        || item.problemCode().toLowerCase(Locale.ROOT).contains(normalizedKeyword)
                        || item.title().toLowerCase(Locale.ROOT).contains(normalizedKeyword)
                        || item.tag().toLowerCase(Locale.ROOT).contains(normalizedKeyword)
                        || item.platform().toLowerCase(Locale.ROOT).contains(normalizedKeyword))
                .filter(item -> minRating == null || item.rating() >= minRating)
                .filter(item -> maxRating == null || item.rating() <= maxRating)
                .filter(item -> solved == null || item.solved() == solved)
                .filter(item -> recommended == null || item.recommended() == recommended)
                .sorted(Comparator
                        .comparing(ProblemCatalogResponse::solved, Comparator.reverseOrder())
                        .thenComparing(ProblemCatalogResponse::recommended, Comparator.reverseOrder())
                        .thenComparing(ProblemCatalogResponse::rating)
                        .thenComparing(ProblemCatalogResponse::problemCode))
                .toList();

        return slicePage(rows, page, size);
    }

    @Cacheable(value = "alerts", key = "#page + '_' + #size")
    public PageResponse<AlertResponse> alerts(Long userId, int page, int size) {
        validateCoach(userId);
        Pageable pageable = PageRequest.of(page, size);
        Page<?> entityPage = alertLogRepository.findAll(pageable);

        List<AlertResponse> content = entityPage.stream()
                .map(e -> DtoMapper.toAlertResponse((com.acmtrain.backend.entity.AlertLogEntity) e))
                .toList();

        return new PageResponse<>(
                content,
                entityPage.getNumber(),
                entityPage.getSize(),
                entityPage.getTotalElements(),
                entityPage.getTotalPages(),
                entityPage.isLast(),
                entityPage.isFirst()
        );
    }

    @Cacheable(value = "alerts", key = "'me_' + #userId + '_' + #page + '_' + #size")
    public PageResponse<AlertResponse> myAlerts(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AlertLogEntity> entityPage = alertLogRepository.findByUserIdOrderByHitTimeDescIdDesc(userId, pageable);

        List<AlertResponse> content = entityPage.stream()
                .map(DtoMapper::toAlertResponse)
                .toList();

        return new PageResponse<>(
                content,
                entityPage.getNumber(),
                entityPage.getSize(),
                entityPage.getTotalElements(),
                entityPage.getTotalPages(),
                entityPage.isLast(),
                entityPage.isFirst()
        );
    }

    @Transactional
    @CacheEvict(value = {"alerts"}, allEntries = true)
    public AlertResponse updateMyAlertFeedback(Long userId, Long alertId, UpdateAlertFeedbackRequest request) {
        UserAccountEntity user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
        if (!"student".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅学生可反馈异常");
        }

        AlertLogEntity alert = alertLogRepository.findByIdAndUserId(alertId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "异常记录不存在"));
        String feedback = request.feedback() == null ? null : request.feedback().trim();
        if (feedback != null && feedback.isEmpty()) {
            feedback = null;
        }

        alert.setStudentFeedback(feedback);
        alert.setFeedbackAt(feedback == null ? null : LocalDateTime.now());
        AlertLogEntity saved = alertLogRepository.save(alert);
        return DtoMapper.toAlertResponse(saved);
    }

    @Cacheable(value = "students", key = "#page + '_' + #size")
    public PageResponse<StudentResponse> students(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<?> entityPage = studentInfoRepository.findAll(pageable);

        List<StudentResponse> content = entityPage.stream()
                .map(e -> toStudentResponse((StudentInfoEntity) e))
                .toList();

        return new PageResponse<>(
                content,
                entityPage.getNumber(),
                entityPage.getSize(),
                entityPage.getTotalElements(),
                entityPage.getTotalPages(),
                entityPage.isLast(),
                entityPage.isFirst()
        );
    }

    @Transactional
    @CacheEvict(value = {"students", "myProfile", "recommendations"}, allEntries = true)
    public StudentResponse createStudent(Long operatorId, CreateStudentRequest request) {
        validateCoach(operatorId);

        String username = normalizeRequired(request.username(), "username");
        if (userAccountRepository.findByUsername(username).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "账号已存在");
        }

        UserAccountEntity user = new UserAccountEntity();
        user.setUsername(username);
        user.setPassword(normalizeRequired(request.password(), "password"));
        user.setRealName(normalizeRequired(request.realName(), "realName"));
        user.setRole("student");
        UserAccountEntity savedUser = userAccountRepository.save(user);

        StudentInfoEntity student = new StudentInfoEntity();
        fillStudentInfo(student, savedUser.getId(), request.realName(), request.grade(), request.major(), request.cfHandle(),
                request.atcHandle(), request.cfRating(), request.atcRating(), request.solvedCount(), request.totalPoints());
        StudentInfoEntity savedStudent = studentInfoRepository.save(student);
        return toStudentResponse(savedStudent, savedUser);
    }

    @Transactional
    @CacheEvict(value = {"students", "myProfile", "recommendations"}, allEntries = true)
    public StudentResponse updateStudent(Long operatorId, Long id, UpdateStudentRequest request) {
        validateCoach(operatorId);

        StudentInfoEntity student = studentInfoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "学生不存在"));
        UserAccountEntity user = userAccountRepository.findById(student.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "学生账号不存在"));

        String username = normalizeRequired(request.username(), "username");
        Optional<UserAccountEntity> duplicated = userAccountRepository.findByUsername(username);
        if (duplicated.isPresent() && !duplicated.get().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "账号已存在");
        }

        user.setUsername(username);
        user.setRealName(normalizeRequired(request.realName(), "realName"));
        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(request.password().trim());
        }
        user.setRole("student");
        UserAccountEntity savedUser = userAccountRepository.save(user);

        fillStudentInfo(student, savedUser.getId(), request.realName(), request.grade(), request.major(), request.cfHandle(),
                request.atcHandle(), request.cfRating(), request.atcRating(), request.solvedCount(), request.totalPoints());
        StudentInfoEntity savedStudent = studentInfoRepository.save(student);
        return toStudentResponse(savedStudent, savedUser);
    }

    private List<ProblemSeed> loadCodeforcesProblemSeeds() {
        return codeforcesCatalogService.loadProblems().stream()
                .filter(problem -> problem.rating() > 0)
                .map(problem -> new ProblemSeed(
                        problem.problemCode(),
                        problem.title(),
                        problem.rating(),
                        normalizeTag(problem.primaryTag()),
                        "Codeforces"
                ))
                .toList();
    }

    private List<RecommendationResponse> buildAlgorithmRecommendations(StudentInfoEntity student) {
        int hiddenRating = computeHiddenRating(student);
        Set<String> solvedCodes = ojSolvedProblemRepository.findAllByUserIdOrderByAcceptedAtDesc(student.getUserId()).stream()
                .map(OjSolvedProblemEntity::getProblemCode)
                .collect(java.util.stream.Collectors.toSet());
        List<ProblemSeed> catalog = loadCodeforcesProblemSeeds();

        int cfRating = student.getCfRating() == null ? 0 : student.getCfRating();
        int solvedCount = student.getSolvedCount() == null ? 0 : student.getSolvedCount();
        int expectedSolved = Math.max(60, cfRating / 12);
        double solvedRatio = expectedSolved == 0 ? 1.0 : solvedCount * 1.0 / expectedSolved;

        String profileReason;
        if (solvedRatio >= 1.35) {
            profileReason = "近期做题量显著高于当前公开分，系统上调了推荐难度。";
        } else if (solvedRatio >= 1.1) {
            profileReason = "近期训练状态稳定，推荐难度略高于当前分段。";
        } else if (solvedRatio <= 0.8) {
            profileReason = "近期训练量偏少，建议先巩固基础再冲刺。";
        } else {
            profileReason = "当前训练节奏与公开分段匹配，继续稳步提升。";
        }

        Set<String> usedCodes = new HashSet<>();
        return IntStream.range(0, RECOMMENDATION_PLANS.size())
                .mapToObj(index -> buildLevelRecommendation(
                        index + 1L,
                        hiddenRating,
                        RECOMMENDATION_PLANS.get(index),
                        profileReason,
                        usedCodes,
                        solvedCodes,
                        catalog
                ))
                .toList();
    }

    private RecommendationResponse buildLevelRecommendation(
            long id,
            int hiddenRating,
            RecommendationPlan plan,
            String profileReason,
            Set<String> usedCodes,
            Set<String> solvedCodes,
            List<ProblemSeed> catalog
    ) {
        int targetRating = clamp(hiddenRating + plan.targetOffset(), 800, 3200);
        ProblemSeed selected = catalog.stream()
                .filter(seed -> matchesRecommendationBand(plan.level(), hiddenRating, seed.rating()))
                .filter(seed -> !usedCodes.contains(seed.code()))
                .filter(seed -> !solvedCodes.contains(seed.code()))
                .min(Comparator.comparingInt(seed -> Math.abs(seed.rating() - targetRating)))
                .orElseGet(() -> catalog.stream()
                        .filter(seed -> !usedCodes.contains(seed.code()))
                        .min(Comparator.comparingInt(seed -> Math.abs(seed.rating() - targetRating)))
                        .orElseGet(() -> catalog.stream()
                                .min(Comparator.comparingInt(seed -> Math.abs(seed.rating() - targetRating)))
                                .orElseThrow()));

        usedCodes.add(selected.code());
        String reason = "隐藏分 " + hiddenRating + "，建议先做 " + selected.rating() + " 附近题目。"
                + plan.levelReason()
                + profileReason;

        return new RecommendationResponse(
                id,
                plan.level(),
                selected.code(),
                selected.title(),
                selected.rating(),
                hiddenRating,
                reason
        );
    }

    private boolean matchesRecommendationBand(String level, int hiddenRating, int rating) {
        return switch (level) {
            case "WARMUP" -> rating <= Math.max(900, hiddenRating - 20);
            case "CORE" -> rating >= Math.max(800, hiddenRating - 40) && rating <= hiddenRating + 140;
            case "CHALLENGE" -> rating >= hiddenRating + 120;
            default -> true;
        };
    }

    private int computeHiddenRating(StudentInfoEntity student) {
        int cf = student.getCfRating() == null ? 0 : student.getCfRating();
        int atc = student.getAtcRating() == null ? 0 : student.getAtcRating();
        int solved = student.getSolvedCount() == null ? 0 : student.getSolvedCount();

        int baseRating = (int) Math.round(cf * 0.7 + atc * 0.3);
        int expectedSolved = Math.max(60, cf / 12);
        double ratio = expectedSolved == 0 ? 1.0 : solved * 1.0 / expectedSolved;

        int solvedBoost = (int) Math.round((ratio - 1.0) * 220);
        solvedBoost = clamp(solvedBoost, -140, 320);

        return clamp(baseRating + solvedBoost, 800, 3200);
    }

    private List<ProblemBucketResponse> buildProblemBuckets(int totalSolved, int hiddenRating) {
        List<String> labels = List.of("800-1199", "1200-1399", "1400-1599", "1600-1899", "1900+");
        List<Integer> targets = List.of(1000, 1300, 1500, 1750, 2050);
        List<Double> weights = targets.stream()
                .map(target -> Math.max(0.12, 1.15 - Math.abs(hiddenRating - target) / 900.0))
                .toList();

        List<Integer> counts = allocateCounts(totalSolved, weights);
        List<ProblemBucketResponse> buckets = new ArrayList<>();
        for (int i = 0; i < labels.size(); i++) {
            int count = counts.get(i);
            int percentage = totalSolved == 0 ? 0 : (int) Math.round(count * 100.0 / totalSolved);
            buckets.add(new ProblemBucketResponse(labels.get(i), count, percentage));
        }
        return buckets;
    }

    private List<ProblemTagResponse> buildProblemTags(int totalSolved, int hiddenRating) {
        List<String> tags = List.of("Greedy", "Implementation", "DP", "Graph", "String");
        double graphWeight = hiddenRating >= 1700 ? 1.05 : 0.8;
        double dpWeight = hiddenRating >= 1600 ? 1.0 : 0.78;
        double stringWeight = hiddenRating >= 1500 ? 0.86 : 0.72;
        List<Double> weights = List.of(
                hiddenRating >= 1450 ? 0.98 : 0.88,
                hiddenRating >= 1450 ? 0.92 : 1.1,
                dpWeight,
                graphWeight,
                stringWeight
        );

        List<Integer> counts = allocateCounts(Math.max(totalSolved, 1), weights);
        List<ProblemTagResponse> distribution = new ArrayList<>();
        for (int i = 0; i < tags.size(); i++) {
            distribution.add(new ProblemTagResponse(tags.get(i), counts.get(i)));
        }
        return distribution;
    }

    private List<ProblemDetailResponse> buildProblemDetails(int hiddenRating, List<ProblemTagResponse> tags) {
        return loadCodeforcesProblemSeeds().stream()
                .sorted(Comparator.comparingInt(seed -> Math.abs(seed.rating() - hiddenRating)))
                .limit(12)
                .map(seed -> new ProblemDetailResponse(
                        seed.code(),
                        seed.title(),
                        seed.rating(),
                        seed.tag(),
                        resolveBucketLabel(seed.rating())
                ))
                .toList();
    }

    private List<ProblemDetailResponse> buildProblemDetails(List<OjSolvedProblemEntity> solvedProblems) {
        return solvedProblems.stream()
                .sorted(Comparator.comparing(OjSolvedProblemEntity::getAcceptedAt).reversed())
                .limit(12)
                .map(problem -> new ProblemDetailResponse(
                        problem.getProblemCode(),
                        problem.getTitle(),
                        problem.getRating() == null ? 0 : problem.getRating(),
                        normalizeTag(problem.getTag()),
                        resolveBucketLabel(problem.getRating() == null ? 0 : problem.getRating())
                ))
                .toList();
    }

    private List<ProblemBucketResponse> buildProblemBuckets(List<OjSolvedProblemEntity> solvedProblems) {
        List<String> labels = List.of("800-1199", "1200-1399", "1400-1599", "1600-1899", "1900+");
        int total = solvedProblems.size();
        List<ProblemBucketResponse> buckets = new ArrayList<>();
        for (String label : labels) {
            int count = (int) solvedProblems.stream()
                    .filter(problem -> label.equals(resolveBucketLabel(problem.getRating() == null ? 0 : problem.getRating())))
                    .count();
            int percentage = total == 0 ? 0 : (int) Math.round(count * 100.0 / total);
            buckets.add(new ProblemBucketResponse(label, count, percentage));
        }
        return buckets;
    }

    private List<ProblemTagResponse> buildProblemTags(List<OjSolvedProblemEntity> solvedProblems) {
        return solvedProblems.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        problem -> normalizeTag(problem.getTag()),
                        java.util.stream.Collectors.counting()
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry::getKey))
                .limit(5)
                .map(entry -> new ProblemTagResponse(entry.getKey(), entry.getValue().intValue()))
                .toList();
    }

    private List<ProblemCatalogResponse> mergeProblemCatalog(
            StudentInfoEntity student,
            int hiddenRating,
            List<OjSolvedProblemEntity> solvedProblems,
            boolean hasRealSolvedData,
            Set<String> solvedCodes,
            Set<String> recommendedCodes
    ) {
        List<ProblemCatalogResponse> rows = new ArrayList<>();
        Set<String> emittedCodes = new HashSet<>();

        for (OjSolvedProblemEntity problem : solvedProblems) {
            if (!emittedCodes.add(problem.getProblemCode())) {
                continue;
            }
            int rating = problem.getRating() == null ? 0 : problem.getRating();
            rows.add(new ProblemCatalogResponse(
                    (long) rows.size() + 1,
                    problem.getProblemCode(),
                    problem.getTitle(),
                    rating,
                    normalizeTag(problem.getTag()),
                    problem.getPlatform(),
                    resolveBucketLabel(rating),
                    true,
                    recommendedCodes.contains(problem.getProblemCode())
            ));
        }

        List<ProblemSeed> catalog = loadCodeforcesProblemSeeds();
        IntStream.range(0, catalog.size())
                .mapToObj(index -> toProblemCatalogResponse(index, catalog.get(index), student, hiddenRating, recommendedCodes, hasRealSolvedData, solvedCodes))
                .filter(item -> emittedCodes.add(item.problemCode()))
                .forEach(rows::add);

        return IntStream.range(0, rows.size())
                .mapToObj(index -> new ProblemCatalogResponse(
                        (long) index + 1,
                        rows.get(index).problemCode(),
                        rows.get(index).title(),
                        rows.get(index).rating(),
                        rows.get(index).tag(),
                        rows.get(index).platform(),
                        rows.get(index).bucketLabel(),
                        rows.get(index).solved(),
                        rows.get(index).recommended()
                ))
                .toList();
    }

    private String normalizeTag(String tag) {
        return tag == null || tag.isBlank() ? "Implementation" : tag;
    }

    private ProblemCatalogResponse toProblemCatalogResponse(
            int index,
            ProblemSeed seed,
            StudentInfoEntity student,
            int hiddenRating,
            Set<String> recommendedCodes,
            boolean hasRealSolvedData,
            Set<String> solvedCodes
    ) {
        boolean solved = student != null && (hasRealSolvedData
                ? solvedCodes.contains(seed.code())
                : estimateSolved(student, seed, hiddenRating));
        return new ProblemCatalogResponse(
                (long) index + 1,
                seed.code(),
                seed.title(),
                seed.rating(),
                seed.tag(),
                seed.platform(),
                resolveBucketLabel(seed.rating()),
                solved,
                recommendedCodes.contains(seed.code())
        );
    }

    private boolean estimateSolved(StudentInfoEntity student, ProblemSeed seed, int hiddenRating) {
        int solvedCount = student.getSolvedCount() == null ? 0 : student.getSolvedCount();
        int delta = hiddenRating - seed.rating();
        int hash = Math.floorMod(seed.code().hashCode(), 100);

        if (delta >= 320) {
            return true;
        }
        if (delta >= 180) {
            return hash < 82;
        }
        if (delta >= 60) {
            return hash < 56;
        }
        if (delta >= -80) {
            return solvedCount >= 80 && hash < 28;
        }
        return false;
    }

    private String resolveBucketLabel(int rating) {
        if (rating < 1200) {
            return "800-1199";
        }
        if (rating < 1400) {
            return "1200-1399";
        }
        if (rating < 1600) {
            return "1400-1599";
        }
        if (rating < 1900) {
            return "1600-1899";
        }
        return "1900+";
    }

    private List<Integer> allocateCounts(int total, List<Double> weights) {
        if (weights.isEmpty()) {
            return List.of();
        }
        if (total <= 0) {
            return weights.stream().map(weight -> 0).toList();
        }

        double weightSum = weights.stream().mapToDouble(Double::doubleValue).sum();
        List<Integer> counts = new ArrayList<>();
        List<Double> remainders = new ArrayList<>();
        int assigned = 0;

        for (double weight : weights) {
            double raw = total * weight / weightSum;
            int floor = (int) Math.floor(raw);
            counts.add(floor);
            remainders.add(raw - floor);
            assigned += floor;
        }

        while (assigned < total) {
            int bestIndex = 0;
            for (int i = 1; i < remainders.size(); i++) {
                if (remainders.get(i) > remainders.get(bestIndex)) {
                    bestIndex = i;
                }
            }
            counts.set(bestIndex, counts.get(bestIndex) + 1);
            remainders.set(bestIndex, 0.0);
            assigned++;
        }

        return counts;
    }

    private RankingMetric resolveRankingMetric(String metric) {
        if (metric == null || metric.isBlank()) {
            return RankingMetric.TOTAL_POINTS;
        }
        try {
            return RankingMetric.valueOf(metric.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "metric 仅支持 TOTAL_POINTS / CF_RATING / ATC_RATING / SOLVED_COUNT"
            );
        }
    }

    private Comparator<RankingOverallEntity> buildRankingComparator(RankingMetric metric) {
        Comparator<RankingOverallEntity> byName = Comparator.comparing(RankingOverallEntity::getUserName);

        return switch (metric) {
            case CF_RATING -> Comparator
                    .comparing(RankingOverallEntity::getCfRating, Comparator.reverseOrder())
                    .thenComparing(byName);
            case ATC_RATING -> Comparator
                    .comparing(RankingOverallEntity::getAtcRating, Comparator.reverseOrder())
                    .thenComparing(byName);
            case SOLVED_COUNT -> Comparator
                    .comparing(RankingOverallEntity::getSolvedCount, Comparator.reverseOrder())
                    .thenComparing(byName);
            case TOTAL_POINTS -> Comparator
                    .comparing(RankingOverallEntity::getTotalPoints, Comparator.reverseOrder())
                    .thenComparing(byName);
        };
    }

    private void validatePage(int page, int size) {
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page 不能小于 0");
        }
        if (size <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size 必须大于 0");
        }
    }

    private int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private <T> PageResponse<T> slicePage(List<T> allRows, int page, int size) {
        int fromIndex = page * size;
        if (fromIndex >= allRows.size()) {
            return new PageResponse<>(
                    List.of(),
                    page,
                    size,
                    allRows.size(),
                    (int) Math.ceil((double) allRows.size() / size),
                    true,
                    page == 0
            );
        }

        int toIndex = Math.min(fromIndex + size, allRows.size());
        List<T> content = allRows.subList(fromIndex, toIndex);
        int totalPages = (int) Math.ceil((double) allRows.size() / size);

        return new PageResponse<>(
                content,
                page,
                size,
                allRows.size(),
                totalPages,
                page >= totalPages - 1,
                page == 0
        );
    }

    private MyProfileResponse toMyProfileResponse(UserAccountEntity user, StudentInfoEntity student) {
        return new MyProfileResponse(
                user.getId(),
                user.getUsername(),
                student.getRealName(),
                student.getGrade(),
                student.getMajor(),
                student.getCfHandle(),
                student.getAtcHandle(),
                student.getCfRating(),
                student.getAtcRating(),
                student.getSolvedCount(),
                student.getTotalPoints()
        );
    }

    private StudentResponse toStudentResponse(StudentInfoEntity student) {
        UserAccountEntity user = userAccountRepository.findById(student.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "学生账号不存在"));
        return toStudentResponse(student, user);
    }

    private StudentResponse toStudentResponse(StudentInfoEntity student, UserAccountEntity user) {
        return new StudentResponse(
                student.getId(),
                student.getUserId(),
                user.getUsername(),
                student.getRealName(),
                student.getGrade(),
                student.getMajor(),
                student.getCfHandle(),
                student.getAtcHandle(),
                student.getCfRating(),
                student.getAtcRating(),
                student.getSolvedCount(),
                student.getTotalPoints()
        );
    }

    private void validateCoach(Long userId) {
        UserAccountEntity operator = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "当前用户不存在"));
        if (!"coach".equalsIgnoreCase(operator.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有教练可以维护学生账号");
        }
    }

    private void fillStudentInfo(
            StudentInfoEntity student,
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
        String normalizedCfHandle = normalizeOptional(cfHandle);
        String normalizedAtcHandle = normalizeOptional(atcHandle);
        validateBoundHandles(normalizedCfHandle, normalizedAtcHandle);
        student.setUserId(userId);
        student.setRealName(normalizeRequired(realName, "realName"));
        student.setGrade(normalizeRequired(grade, "grade"));
        student.setMajor(normalizeRequired(major, "major"));
        student.setCfHandle(normalizedCfHandle);
        student.setAtcHandle(normalizedAtcHandle);
        student.setCfRating(defaultNumber(cfRating));
        student.setAtcRating(defaultNumber(atcRating));
        student.setSolvedCount(defaultNumber(solvedCount));
        student.setTotalPoints(defaultPoints(totalPoints));
    }

    private void validateBoundHandles(String cfHandle, String atcHandle) {
        if (cfHandle != null && !cfHandle.isBlank()) {
            ojAccountValidationService.validateCodeforcesHandle(cfHandle);
        }
        if (atcHandle != null && !atcHandle.isBlank()) {
            ojAccountValidationService.validateAtCoderHandle(atcHandle);
        }
    }

    private String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " 不能为空");
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Integer defaultNumber(Integer value) {
        return value == null ? 0 : value;
    }

    private BigDecimal defaultPoints(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP) : value.setScale(1, RoundingMode.HALF_UP);
    }

    private boolean equalsIgnoreCase(String left, String right) {
        if (left == null) {
            return right == null;
        }
        return right != null && left.equalsIgnoreCase(right);
    }

    private LocalDateTime parseDateTime(String input) {
        try {
            return LocalDateTime.parse(input, DATETIME_INPUT);
        } catch (DateTimeParseException ex) {
            try {
                return LocalDateTime.parse(input);
            } catch (DateTimeParseException ignored) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "deadline 格式错误，应为 yyyy-MM-dd HH:mm");
            }
        }
    }
}
