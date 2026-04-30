package com.acmtrain.backend.service;

import com.acmtrain.backend.dto.MyProfileResponse;
import com.acmtrain.backend.dto.OjContestHistoryResponse;
import com.acmtrain.backend.dto.StudentResponse;
import com.acmtrain.backend.entity.AlertLogEntity;
import com.acmtrain.backend.entity.OjContestHistoryEntity;
import com.acmtrain.backend.entity.OjSolvedProblemEntity;
import com.acmtrain.backend.entity.PointLogEntity;
import com.acmtrain.backend.entity.RankingOverallEntity;
import com.acmtrain.backend.entity.StudentInfoEntity;
import com.acmtrain.backend.entity.UserAccountEntity;
import com.acmtrain.backend.repository.AlertLogRepository;
import com.acmtrain.backend.repository.OjContestHistoryRepository;
import com.acmtrain.backend.repository.OjSolvedProblemRepository;
import com.acmtrain.backend.repository.PointLogRepository;
import com.acmtrain.backend.repository.RankingOverallRepository;
import com.acmtrain.backend.repository.StudentInfoRepository;
import com.acmtrain.backend.repository.UserAccountRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Service
public class OjSyncService {

    private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");
    private static final Logger logger = LoggerFactory.getLogger(OjSyncService.class);
    private static final Duration DEFAULT_HTTP_TIMEOUT = Duration.ofSeconds(20);
    private static final Pattern ATCODER_RATING_ROW = Pattern.compile(
            "(?is)<th[^>]*>\\s*Rating\\s*</th>\\s*<td[^>]*>(.*?)</td>"
    );
    private static final List<String> ATCODER_PROBLEM_MODEL_FALLBACK_URLS = List.of(
            "https://raw.githubusercontent.com/kenkoooo/AtCoderProblems/master/lambda-functions/time-estimator/problem-models.json",
            "https://cdn.jsdelivr.net/gh/kenkoooo/AtCoderProblems@master/lambda-functions/time-estimator/problem-models.json"
    );

    private final StudentInfoRepository studentInfoRepository;
    private final UserAccountRepository userAccountRepository;
    private final RankingOverallRepository rankingOverallRepository;
    private final OjContestHistoryRepository ojContestHistoryRepository;
    private final OjSolvedProblemRepository ojSolvedProblemRepository;
    private final PointLogRepository pointLogRepository;
    private final AlertLogRepository alertLogRepository;
    private final AlertNotificationService alertNotificationService;
    private final CodeforcesCatalogService codeforcesCatalogService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final boolean schedulerEnabled;
    private final boolean atcSubmissionsEnabled;
    private final List<String> atcSubmissionsApiBaseUrls;
    private final List<String> atcProblemApiBaseUrls;
    private final long atcRequestIntervalMs;
    private final Object atcRequestLock = new Object();
    private final Object atcProblemMetaLock = new Object();
    private final Map<Long, Object> userSyncLocks = new ConcurrentHashMap<>();
    private volatile long lastAtcRequestAtMs = 0L;
    private volatile Instant atcProblemMetaLoadedAt = Instant.EPOCH;
    private volatile Map<String, AtCoderProblemMeta> atcProblemMetaMap = Map.of();

    public OjSyncService(
            StudentInfoRepository studentInfoRepository,
            UserAccountRepository userAccountRepository,
            RankingOverallRepository rankingOverallRepository,
            OjContestHistoryRepository ojContestHistoryRepository,
            OjSolvedProblemRepository ojSolvedProblemRepository,
            PointLogRepository pointLogRepository,
            AlertLogRepository alertLogRepository,
            AlertNotificationService alertNotificationService,
            CodeforcesCatalogService codeforcesCatalogService,
            ObjectMapper objectMapper,
            @Value("${acm.sync.scheduler-enabled:false}") boolean schedulerEnabled,
            @Value("${acm.sync.atc-submissions-enabled:true}") boolean atcSubmissionsEnabled,
            @Value("${acm.sync.atc-submissions-api-base-url:https://kenkoooo.com/atcoder/atcoder-api/v3}") String atcSubmissionsApiBaseUrl,
            @Value("${acm.sync.atc-problem-api-base-url:https://kenkoooo.com/atcoder/resources}") String atcProblemApiBaseUrl,
            @Value("${acm.sync.atc-request-interval-ms:1200}") long atcRequestIntervalMs
    ) {
        this.studentInfoRepository = studentInfoRepository;
        this.userAccountRepository = userAccountRepository;
        this.rankingOverallRepository = rankingOverallRepository;
        this.ojContestHistoryRepository = ojContestHistoryRepository;
        this.ojSolvedProblemRepository = ojSolvedProblemRepository;
        this.pointLogRepository = pointLogRepository;
        this.alertLogRepository = alertLogRepository;
        this.alertNotificationService = alertNotificationService;
        this.codeforcesCatalogService = codeforcesCatalogService;
        this.objectMapper = objectMapper;
        this.schedulerEnabled = schedulerEnabled;
        this.atcSubmissionsEnabled = atcSubmissionsEnabled;
        this.atcSubmissionsApiBaseUrls = parseBaseUrls(
                atcSubmissionsApiBaseUrl,
                "https://kenkoooo.com/atcoder/atcoder-api/v3"
        );
        this.atcProblemApiBaseUrls = parseBaseUrls(
                atcProblemApiBaseUrl,
                "https://kenkoooo.com/atcoder/resources"
        );
        this.atcRequestIntervalMs = Math.max(atcRequestIntervalMs, 1000L);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(DEFAULT_HTTP_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Transactional
    @CacheEvict(value = {"myProfile", "students", "recommendations", "problems", "rankings", "alerts", "points"}, allEntries = true)
    public MyProfileResponse syncMyProfile(Long userId) {
        return executeUnderUserLock(userId, () -> {
            StudentInfoEntity student = findStudentByUserId(userId);
            UserAccountEntity user = findUser(userId);
            SyncedSnapshot snapshot = syncStudent(student, user);
            return toMyProfileResponse(user, snapshot.student());
        });
    }

    @Transactional
    @CacheEvict(value = {"myProfile", "students", "recommendations", "problems", "rankings", "alerts", "points"}, allEntries = true)
    public StudentResponse syncStudentById(Long operatorId, Long studentId) {
        UserAccountEntity operator = findUser(operatorId);
        if (!"coach".equalsIgnoreCase(operator.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有教练可以同步学生 OJ 数据");
        }

        StudentInfoEntity student = studentInfoRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "学生不存在"));
        return executeUnderUserLock(student.getUserId(), () -> {
            UserAccountEntity user = findUser(student.getUserId());
            SyncedSnapshot snapshot = syncStudent(student, user);
            return toStudentResponse(snapshot.student(), user);
        });
    }

    public List<OjContestHistoryResponse> getMyContestHistory(Long userId) {
        return ojContestHistoryRepository.findTop20ByUserIdOrderByContestTimeDesc(userId).stream()
                .map(this::toContestHistoryResponse)
                .toList();
    }

    @Transactional
    @CacheEvict(value = {"myProfile", "students", "recommendations", "problems", "rankings", "alerts", "points"}, allEntries = true)
    public MyProfileResponse importMyAtCoderSubmissions(Long userId, MultipartFile file) {
        return executeUnderUserLock(userId, () -> {
            StudentInfoEntity student = findStudentByUserId(userId);
            UserAccountEntity user = findUser(userId);
            importAtCoderSolvedProblems(user, student, file);
            StudentInfoEntity refreshed = studentInfoRepository.save(student);
            updateRanking(user, refreshed);
            return toMyProfileResponse(user, refreshed);
        });
    }

    @Transactional
    @CacheEvict(value = {"myProfile", "students", "recommendations", "problems", "rankings", "alerts", "points"}, allEntries = true)
    public StudentResponse importStudentAtCoderSubmissions(Long operatorId, Long studentId, MultipartFile file) {
        UserAccountEntity operator = findUser(operatorId);
        if (!"coach".equalsIgnoreCase(operator.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有教练可以导入学生 AtCoder 提交记录");
        }

        StudentInfoEntity student = studentInfoRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "学生不存在"));
        return executeUnderUserLock(student.getUserId(), () -> {
            UserAccountEntity user = findUser(student.getUserId());
            importAtCoderSolvedProblems(user, student, file);
            StudentInfoEntity refreshed = studentInfoRepository.save(student);
            updateRanking(user, refreshed);
            return toStudentResponse(refreshed, user);
        });
    }

    @Scheduled(cron = "${acm.sync.cron:0 0 */6 * * *}")
    @Transactional
    @CacheEvict(value = {"myProfile", "students", "recommendations", "problems", "rankings", "alerts", "points"}, allEntries = true)
    public void scheduledSyncAllStudents() {
        if (!schedulerEnabled) {
            return;
        }

        int success = 0;
        int skipped = 0;
        for (StudentInfoEntity student : studentInfoRepository.findAllByOrderByIdAsc()) {
            try {
                UserAccountEntity user = findUser(student.getUserId());
                boolean missingCf = student.getCfHandle() == null || student.getCfHandle().isBlank();
                boolean missingAtc = student.getAtcHandle() == null || student.getAtcHandle().isBlank();
                if (missingCf && missingAtc) {
                    skipped++;
                    continue;
                }
                syncStudent(student, user);
                success++;
            } catch (ResponseStatusException ex) {
                skipped++;
                logger.warn("Scheduled OJ sync skipped for userId={}, reason={}", student.getUserId(), ex.getReason());
            } catch (Exception ex) {
                skipped++;
                logger.warn("Scheduled OJ sync failed for userId={}", student.getUserId(), ex);
            }
        }
        logger.info("Scheduled OJ sync finished: success={}, skipped={}", success, skipped);
    }

    private SyncedSnapshot syncStudent(StudentInfoEntity student, UserAccountEntity user) {
        boolean cfHandleChanged = isHandleChanged(student.getCfHandle(), student.getCfSyncedHandle());
        boolean atcHandleChanged = isKnownHandleChanged(student.getAtcHandle(), student.getAtcSyncedHandle());

        CodeforcesProfile cfProfile = fetchCodeforcesProfile(
                student.getCfHandle(),
                student.getCfLastSubmissionEpochSecond(),
                cfHandleChanged
        );
        AtCoderProfile atCoderProfile = fetchAtCoderProfile(
                student.getAtcHandle(),
                student.getAtcLastSubmissionEpochSecond(),
                atcHandleChanged
        );
        List<AcceptedProblemSeed> cfAcceptedProblems = syncCodeforcesAcceptedProblems(user.getId(), student, cfProfile, cfHandleChanged);
        List<AcceptedProblemSeed> atCoderAcceptedProblems = syncAtCoderAcceptedProblems(user.getId(), student, atCoderProfile, atcHandleChanged);
        List<AcceptedProblemSeed> mergedAcceptedProblems = mergeAcceptedProblems(cfAcceptedProblems, atCoderAcceptedProblems);
        List<ContestHistorySeed> mergedHistory = mergeContestHistory(cfProfile.history(), atCoderProfile.history());

        student.setCfRating(cfProfile.rating());
        student.setAtcRating(atCoderProfile.rating());
        student.setSolvedCount(mergedAcceptedProblems.size());
        student.setTotalPoints(settlePoints(user, student, mergedAcceptedProblems, mergedHistory));
        StudentInfoEntity savedStudent = studentInfoRepository.save(student);

        replaceContestHistory(user.getId(), cfProfile.history(), atCoderProfile.history());
        updateRanking(user, savedStudent);
        refreshAlerts(savedStudent, mergedAcceptedProblems);
        alertNotificationService.sendPendingAlertsIfEnabled();

        return new SyncedSnapshot(savedStudent, user);
    }

    private List<AcceptedProblemSeed> syncCodeforcesAcceptedProblems(
            Long userId,
            StudentInfoEntity student,
            CodeforcesProfile profile,
            boolean handleChanged
    ) {
        if (student.getCfHandle() == null || student.getCfHandle().isBlank()) {
            ojSolvedProblemRepository.deleteByUserIdAndPlatform(userId, "Codeforces");
            student.setCfLastSubmissionEpochSecond(null);
            student.setCfSyncedHandle(null);
            return List.of();
        }

        if (handleChanged || profile.fullRefresh()) {
            replaceSolvedProblemsByPlatform(userId, "Codeforces", profile.acceptedProblems());
        } else {
            appendSolvedProblemsByPlatform(userId, "Codeforces", profile.acceptedProblems());
        }

        student.setCfSyncedHandle(student.getCfHandle().trim());
        if (profile.latestSubmissionEpochSecond() != null && profile.latestSubmissionEpochSecond() > 0) {
            student.setCfLastSubmissionEpochSecond(profile.latestSubmissionEpochSecond());
        }
        return loadAcceptedProblemSeedsByPlatform(userId, "Codeforces");
    }

    private List<AcceptedProblemSeed> syncAtCoderAcceptedProblems(
            Long userId,
            StudentInfoEntity student,
            AtCoderProfile profile,
            boolean handleChanged
    ) {
        if (student.getAtcHandle() == null || student.getAtcHandle().isBlank()) {
            ojSolvedProblemRepository.deleteByUserIdAndPlatform(userId, "AtCoder");
            student.setAtcSyncedHandle(null);
            student.setAtcLastSubmissionEpochSecond(null);
            return List.of();
        }
        if (handleChanged) {
            ojSolvedProblemRepository.deleteByUserIdAndPlatform(userId, "AtCoder");
        }

        if (profile.acceptedProblemsFetched()) {
            if (handleChanged || profile.fullRefresh()) {
                replaceSolvedProblemsByPlatform(userId, "AtCoder", profile.acceptedProblems());
            } else {
                appendSolvedProblemsByPlatform(userId, "AtCoder", profile.acceptedProblems());
            }
            if (profile.latestSubmissionEpochSecond() != null && profile.latestSubmissionEpochSecond() > 0) {
                student.setAtcLastSubmissionEpochSecond(profile.latestSubmissionEpochSecond());
            }
        } else if (handleChanged) {
            student.setAtcLastSubmissionEpochSecond(null);
        }

        student.setAtcSyncedHandle(student.getAtcHandle().trim());
        return loadAcceptedProblemSeedsByPlatform(userId, "AtCoder");
    }

    private List<AcceptedProblemSeed> mergeAcceptedProblems(List<AcceptedProblemSeed> cfProblems, List<AcceptedProblemSeed> atCoderProblems) {
        List<AcceptedProblemSeed> merged = new ArrayList<>(cfProblems.size() + atCoderProblems.size());
        merged.addAll(cfProblems);
        merged.addAll(atCoderProblems);
        merged.sort(Comparator.comparing(AcceptedProblemSeed::acceptedAt).reversed());
        return merged;
    }

    private List<ContestHistorySeed> mergeContestHistory(List<ContestHistorySeed> cfHistory, List<ContestHistorySeed> atCoderHistory) {
        List<ContestHistorySeed> merged = new ArrayList<>(cfHistory.size() + atCoderHistory.size());
        merged.addAll(cfHistory);
        merged.addAll(atCoderHistory);
        merged.sort(Comparator.comparing(ContestHistorySeed::contestTime).reversed());
        return merged;
    }

    private void replaceContestHistory(Long userId, List<ContestHistorySeed> cfHistory, List<ContestHistorySeed> atCoderHistory) {
        List<OjContestHistoryEntity> entities = new ArrayList<>();
        cfHistory.forEach(item -> entities.add(toContestHistoryEntity(userId, item)));
        atCoderHistory.forEach(item -> entities.add(toContestHistoryEntity(userId, item)));
        entities.sort(Comparator.comparing(OjContestHistoryEntity::getContestTime).reversed());

        ojContestHistoryRepository.deleteByUserId(userId);
        ojContestHistoryRepository.saveAll(entities.stream().limit(20).toList());
    }

    private OjContestHistoryEntity toContestHistoryEntity(Long userId, ContestHistorySeed item) {
        OjContestHistoryEntity entity = new OjContestHistoryEntity();
        entity.setUserId(userId);
        entity.setPlatform(item.platform());
        entity.setContestName(item.contestName());
        entity.setContestUrl(item.contestUrl());
        entity.setContestTime(item.contestTime());
        entity.setRankNo(item.rankNo());
        entity.setPerformance(item.performance());
        entity.setNewRating(item.newRating());
        entity.setRatingChange(item.ratingChange());
        return entity;
    }

    private void replaceSolvedProblemsByPlatform(Long userId, String platform, List<AcceptedProblemSeed> acceptedProblems) {
        ojSolvedProblemRepository.deleteByUserIdAndPlatform(userId, platform);
        if (acceptedProblems.isEmpty()) {
            return;
        }

        List<OjSolvedProblemEntity> entities = acceptedProblems.stream()
                .filter(problem -> platform.equals(problem.platform()))
                .map(problem -> toSolvedProblemEntity(userId, problem))
                .toList();
        ojSolvedProblemRepository.saveAll(entities);
    }

    private void appendSolvedProblemsByPlatform(Long userId, String platform, List<AcceptedProblemSeed> acceptedProblems) {
        if (acceptedProblems.isEmpty()) {
            return;
        }

        Set<String> existingKeys = new HashSet<>();
        ojSolvedProblemRepository.findAllByUserIdAndPlatformOrderByAcceptedAtDesc(userId, platform)
                .forEach(problem -> existingKeys.add(problem.getSourceKey()));

        List<OjSolvedProblemEntity> newEntities = acceptedProblems.stream()
                .filter(problem -> platform.equals(problem.platform()))
                .filter(problem -> !existingKeys.contains(toStoredSourceKey(userId, problem.sourceKey())))
                .map(problem -> toSolvedProblemEntity(userId, problem))
                .toList();
        if (!newEntities.isEmpty()) {
            ojSolvedProblemRepository.saveAll(newEntities);
        }
    }

    private void importAtCoderSolvedProblems(UserAccountEntity user, StudentInfoEntity student, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请上传 AtCoder 提交记录 JSON");
        }
        if (student.getAtcHandle() == null || student.getAtcHandle().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前学生未绑定 AtCoder 账号");
        }

        List<AcceptedProblemSeed> importedProblems = parseImportedAtCoderProblems(file, student.getAtcHandle());
        if (importedProblems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "未解析到通过记录，请检查 JSON 内容");
        }

        replaceSolvedProblemsByPlatform(user.getId(), "AtCoder", importedProblems);
        student.setAtcSyncedHandle(student.getAtcHandle().trim());
        student.setAtcLastSubmissionEpochSecond(importedProblems.stream()
                .map(AcceptedProblemSeed::acceptedAt)
                .map(item -> item.atZone(SHANGHAI_ZONE).toEpochSecond())
                .max(Long::compareTo)
                .orElse(null));
        student.setSolvedCount((int) ojSolvedProblemRepository.countByUserId(user.getId()));
        List<AcceptedProblemSeed> allAcceptedProblems = loadAcceptedProblemSeeds(user.getId());
        student.setTotalPoints(settlePoints(user, student, allAcceptedProblems, List.of()));
        refreshAlerts(student, allAcceptedProblems);
        alertNotificationService.sendPendingAlertsIfEnabled();
    }

    private OjSolvedProblemEntity toSolvedProblemEntity(Long userId, AcceptedProblemSeed problem) {
        OjSolvedProblemEntity entity = new OjSolvedProblemEntity();
        entity.setUserId(userId);
        entity.setPlatform(problem.platform());
        entity.setProblemCode(problem.problemCode());
        entity.setTitle(problem.title());
        entity.setProblemUrl(problem.problemUrl());
        entity.setRating(problem.rating() <= 0 ? null : problem.rating());
        entity.setTag(problem.primaryTag());
        entity.setAcceptedAt(problem.acceptedAt());
        entity.setSourceKey(toStoredSourceKey(userId, problem.sourceKey()));
        return entity;
    }

    private List<AcceptedProblemSeed> parseImportedAtCoderProblems(MultipartFile file, String atcHandle) {
        try {
            JsonNode root = objectMapper.readTree(file.getInputStream());
            JsonNode submissions = extractSubmissionArray(root);
            if (submissions == null || !submissions.isArray()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "JSON 格式不正确，缺少 submissions 数组");
            }

            Map<String, AcceptedProblemSeed> solved = new HashMap<>();
            for (JsonNode item : submissions) {
                if (!isAcceptedSubmission(item)) {
                    continue;
                }
                if (!matchesAtCoderUser(item, atcHandle)) {
                    continue;
                }

                String taskId = firstNonBlank(
                        textValue(item, "problem_id"),
                        textValue(item, "task_id"),
                        textValue(item, "problemId"),
                        textValue(item, "taskId"),
                        textValue(item, "problem_code")
                );
                if (taskId == null || taskId.isBlank()) {
                    continue;
                }
                taskId = taskId.trim().toLowerCase(Locale.ROOT);
                String contestId = firstNonBlank(
                        textValue(item, "contest_id"),
                        textValue(item, "contestId"),
                        deriveAtCoderContestId(taskId)
                );
                if (contestId == null || contestId.isBlank()) {
                    continue;
                }

                LocalDateTime acceptedAt = parseImportedAcceptedAt(item);
                String title = firstNonBlank(
                        textValue(item, "title"),
                        textValue(item, "problem_title"),
                        textValue(item, "problemTitle"),
                        textValue(item, "name"),
                        taskId
                );
                Integer rating = integerValue(item, "rating", "difficulty");
                String tag = firstNonBlank(
                        textValue(item, "tag"),
                        textValue(item, "genre"),
                        textValue(item, "category"),
                        "Implementation"
                );

                AcceptedProblemSeed candidate = new AcceptedProblemSeed(
                        "AtCoder",
                        "ATC " + taskId,
                        title,
                        "https://atcoder.jp/contests/" + contestId + "/tasks/" + taskId,
                        rating == null ? 0 : rating,
                        tag,
                        List.of(tag),
                        acceptedAt,
                        "ATC:" + contestId + ":" + taskId
                );

                solved.merge(taskId, candidate, (left, right) ->
                        left.acceptedAt().isAfter(right.acceptedAt()) ? right : left);
            }

            return solved.values().stream()
                    .sorted(Comparator.comparing(AcceptedProblemSeed::acceptedAt).reversed())
                    .toList();
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "读取 JSON 文件失败");
        }
    }

    private void updateRanking(UserAccountEntity user, StudentInfoEntity student) {
        List<RankingOverallEntity> rankings = new ArrayList<>(rankingOverallRepository.findAll());
        Optional<RankingOverallEntity> existing = rankings.stream()
                .filter(item -> item.getUserName().equals(student.getRealName()))
                .findFirst();

        RankingOverallEntity target = existing.orElseGet(() -> {
            RankingOverallEntity created = new RankingOverallEntity();
            created.setUserName(student.getRealName());
            created.setStreakDays(1);
            rankings.add(created);
            return created;
        });

        target.setUserName(student.getRealName());
        target.setCfRating(student.getCfRating());
        target.setAtcRating(student.getAtcRating());
        target.setSolvedCount(student.getSolvedCount());
        target.setTotalPoints(student.getTotalPoints());
        target.setStreakDays(Math.max(target.getStreakDays() == null ? 1 : target.getStreakDays(), 1));

        rankings.sort(Comparator
                .comparing(RankingOverallEntity::getTotalPoints, Comparator.reverseOrder())
                .thenComparing(RankingOverallEntity::getCfRating, Comparator.reverseOrder())
                .thenComparing(RankingOverallEntity::getAtcRating, Comparator.reverseOrder())
                .thenComparing(RankingOverallEntity::getSolvedCount, Comparator.reverseOrder())
                .thenComparing(RankingOverallEntity::getUserName));

        for (int i = 0; i < rankings.size(); i++) {
            rankings.get(i).setRankNo(i + 1);
        }
        rankingOverallRepository.saveAll(rankings);
    }

    private CodeforcesProfile fetchCodeforcesProfile(String handle, Long lastSubmissionEpochSecond, boolean forceFullRefresh) {
        if (handle == null || handle.isBlank()) {
            return new CodeforcesProfile(0, 0, List.of(), List.of(), null, true);
        }
        String normalized = normalizeRequiredHandle(handle, "Codeforces");
        JsonNode infoRoot = readJson("https://codeforces.com/api/user.info?handles="
                + urlEncode(normalized) + "&checkHistoricHandles=false");
        ensureApiOk(infoRoot, "Codeforces 用户信息获取失败");
        JsonNode info = infoRoot.path("result").path(0);
        if (info.isMissingNode()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Codeforces 账号不存在或不可访问");
        }

        int rating = info.path("rating").asInt(0);
        CodeforcesSubmissionBatch submissionBatch = fetchCodeforcesAcceptedProblems(
                normalized,
                lastSubmissionEpochSecond,
                forceFullRefresh
        );
        List<AcceptedProblemSeed> acceptedProblems = submissionBatch.acceptedProblems();
        int solvedCount = acceptedProblems.size();

        JsonNode ratingRoot = readJson("https://codeforces.com/api/user.rating?handle=" + urlEncode(normalized));
        ensureApiOk(ratingRoot, "Codeforces 比赛历史获取失败");
        List<ContestHistorySeed> history = parseCodeforcesHistory(ratingRoot.path("result"));
        return new CodeforcesProfile(
                rating,
                solvedCount,
                history,
                acceptedProblems,
                submissionBatch.latestSubmissionEpochSecond(),
                forceFullRefresh || lastSubmissionEpochSecond == null || lastSubmissionEpochSecond <= 0
        );
    }

    private CodeforcesSubmissionBatch fetchCodeforcesAcceptedProblems(
            String handle,
            Long lastSubmissionEpochSecond,
            boolean forceFullRefresh
    ) {
        final int pageSize = 2000;
        Map<String, AcceptedProblemSeed> solved = new HashMap<>();
        Map<String, CodeforcesCatalogService.CatalogProblem> problemCatalog = codeforcesCatalogService.loadProblemMap();
        Long latestSubmissionEpochSecond = null;
        int from = 1;

        while (true) {
            JsonNode statusRoot = readJson("https://codeforces.com/api/user.status?handle="
                    + urlEncode(handle) + "&from=" + from + "&count=" + pageSize);
            ensureApiOk(statusRoot, "Codeforces 提交记录获取失败");
            JsonNode result = statusRoot.path("result");
            if (!result.isArray() || result.isEmpty()) {
                break;
            }
            if (latestSubmissionEpochSecond == null) {
                latestSubmissionEpochSecond = result.path(0).path("creationTimeSeconds").asLong(0);
            }

            boolean reachedKnownCursor = false;
            for (JsonNode item : result) {
                long creationTimeSeconds = item.path("creationTimeSeconds").asLong(0);
                if (!forceFullRefresh
                        && lastSubmissionEpochSecond != null
                        && lastSubmissionEpochSecond > 0
                        && creationTimeSeconds <= lastSubmissionEpochSecond) {
                    reachedKnownCursor = true;
                    break;
                }
                mergeCodeforcesAcceptedProblem(solved, problemCatalog, item);
            }

            if (reachedKnownCursor || result.size() < pageSize) {
                break;
            }
            from += pageSize;
        }
        return new CodeforcesSubmissionBatch(
                solved.values().stream()
                .sorted(Comparator.comparing(AcceptedProblemSeed::acceptedAt).reversed())
                .toList(),
                latestSubmissionEpochSecond
        );
    }

    private void mergeCodeforcesAcceptedProblem(
            Map<String, AcceptedProblemSeed> solved,
            Map<String, CodeforcesCatalogService.CatalogProblem> problemCatalog,
            JsonNode item
    ) {
        if (!"OK".equalsIgnoreCase(item.path("verdict").asText())) {
            return;
        }
        JsonNode problem = item.path("problem");
        String contestId = problem.path("contestId").isMissingNode() ? "" : problem.path("contestId").asText("");
        String index = problem.path("index").asText("");
        String key = buildProblemKey(contestId, index);
        if (key.isBlank()) {
            return;
        }

        CodeforcesCatalogService.CatalogProblem meta = problemCatalog.getOrDefault(
                key,
                new CodeforcesCatalogService.CatalogProblem(
                        formatCodeforcesProblemCode(contestId, index),
                        problem.path("name").asText("Codeforces Problem"),
                        0,
                        "Implementation",
                        List.of(),
                        buildCodeforcesProblemUrl(contestId, index)
                )
        );
        AcceptedProblemSeed candidate = new AcceptedProblemSeed(
                "Codeforces",
                meta.problemCode(),
                meta.title(),
                meta.url(),
                meta.rating(),
                meta.primaryTag(),
                meta.tags(),
                LocalDateTime.ofInstant(Instant.ofEpochSecond(item.path("creationTimeSeconds").asLong()), SHANGHAI_ZONE),
                "CF:" + key
        );
        solved.merge(key, candidate, (left, right) ->
                left.acceptedAt().isBefore(right.acceptedAt()) ? left : right);
    }

    private List<ContestHistorySeed> parseCodeforcesHistory(JsonNode result) {
        List<ContestHistorySeed> history = new ArrayList<>();
        int size = result.size();
        for (int i = Math.max(0, size - 10); i < size; i++) {
            JsonNode item = result.get(i);
            int newRating = item.path("newRating").asInt(0);
            int oldRating = item.path("oldRating").asInt(newRating);
            history.add(new ContestHistorySeed(
                    "Codeforces",
                    item.path("contestName").asText("Codeforces Contest"),
                    "https://codeforces.com/contest/" + item.path("contestId").asText(),
                    LocalDateTime.ofInstant(Instant.ofEpochSecond(item.path("ratingUpdateTimeSeconds").asLong()), SHANGHAI_ZONE),
                    item.path("rank").asInt(),
                    null,
                    newRating == 0 ? null : newRating,
                    newRating == 0 ? null : newRating - oldRating
            ));
        }
        history.sort(Comparator.comparing(ContestHistorySeed::contestTime).reversed());
        return history;
    }

    private AtCoderProfile fetchAtCoderProfile(String handle, Long lastSubmissionEpochSecond, boolean forceFullRefresh) {
        if (handle == null || handle.isBlank()) {
            return new AtCoderProfile(0, List.of(), List.of(), null, true, true);
        }
        String normalized = handle.trim();
        String profileHtml = readString("https://atcoder.jp/users/" + urlEncode(normalized));
        ensureAtCoderUserExists(profileHtml);
        int rating = parseAtCoderCurrentRating(profileHtml);
        List<ContestHistorySeed> history = parseAtCoderHistoryJson(normalized);
        AtCoderSubmissionBatch submissionBatch = fetchAtCoderAcceptedProblems(
                normalized,
                lastSubmissionEpochSecond,
                forceFullRefresh
        );
        return new AtCoderProfile(
                rating,
                history,
                submissionBatch.acceptedProblems(),
                submissionBatch.latestSubmissionEpochSecond(),
                submissionBatch.fetchedSuccessfully(),
                forceFullRefresh || lastSubmissionEpochSecond == null || lastSubmissionEpochSecond <= 0
        );
    }

    private AtCoderSubmissionBatch fetchAtCoderAcceptedProblems(
            String handle,
            Long lastSubmissionEpochSecond,
            boolean forceFullRefresh
    ) {
        if (!atcSubmissionsEnabled) {
            return new AtCoderSubmissionBatch(List.of(), lastSubmissionEpochSecond, false);
        }

        Map<String, AtCoderProblemMeta> metaMap = loadAtCoderProblemMetaMap();
        Map<String, AcceptedProblemSeed> solved = new HashMap<>();
        Long latestSubmissionEpochSecond = null;
        long initialFromSecond = forceFullRefresh || lastSubmissionEpochSecond == null || lastSubmissionEpochSecond <= 0
                ? 0L
                : lastSubmissionEpochSecond + 1;
        Exception lastException = null;

        for (String baseUrl : atcSubmissionsApiBaseUrls) {
            long fromSecond = initialFromSecond;
            solved.clear();
            latestSubmissionEpochSecond = null;
            try {
                while (true) {
                    JsonNode root = readAtCoderPublicJson(baseUrl
                            + "/user/submissions?user=" + urlEncode(handle) + "&from_second=" + fromSecond);
                    if (!root.isArray() || root.isEmpty()) {
                        break;
                    }

                    for (JsonNode item : root) {
                        long epochSecond = item.path("epoch_second").asLong(0L);
                        if (latestSubmissionEpochSecond == null || epochSecond > latestSubmissionEpochSecond) {
                            latestSubmissionEpochSecond = epochSecond;
                        }
                        mergeAtCoderAcceptedProblem(solved, metaMap, item);
                        fromSecond = Math.max(fromSecond, epochSecond + 1);
                    }

                    if (root.size() < 500) {
                        break;
                    }
                }

                return new AtCoderSubmissionBatch(
                        solved.values().stream()
                                .sorted(Comparator.comparing(AcceptedProblemSeed::acceptedAt).reversed())
                                .toList(),
                        latestSubmissionEpochSecond == null ? lastSubmissionEpochSecond : latestSubmissionEpochSecond,
                        true
                );
            } catch (Exception ex) {
                lastException = ex;
                logger.warn(
                        "AtCoder submissions sync unavailable for handle={}, source={}, reason={}",
                        handle,
                        baseUrl,
                        ex.getMessage()
                );
            }
        }
        return new AtCoderSubmissionBatch(List.of(), lastSubmissionEpochSecond, false);
    }

    private void mergeAtCoderAcceptedProblem(
            Map<String, AcceptedProblemSeed> solved,
            Map<String, AtCoderProblemMeta> metaMap,
            JsonNode item
    ) {
        if (!isAcceptedSubmission(item)) {
            return;
        }

        String taskId = firstNonBlank(
                textValue(item, "problem_id"),
                textValue(item, "task_id"),
                textValue(item, "problemId"),
                textValue(item, "taskId")
        );
        if (taskId == null || taskId.isBlank()) {
            return;
        }
        taskId = taskId.trim().toLowerCase(Locale.ROOT);

        AtCoderProblemMeta meta = metaMap.get(taskId);
        String contestId = firstNonBlank(
                textValue(item, "contest_id"),
                meta == null ? null : meta.contestId(),
                deriveAtCoderContestId(taskId)
        );
        if (contestId == null || contestId.isBlank()) {
            return;
        }

        LocalDateTime acceptedAt = parseImportedAcceptedAt(item);
        AcceptedProblemSeed candidate = new AcceptedProblemSeed(
                "AtCoder",
                "ATC " + taskId,
                meta == null ? taskId : meta.title(),
                "https://atcoder.jp/contests/" + contestId + "/tasks/" + taskId,
                meta == null ? 0 : meta.rating(),
                meta == null ? "Implementation" : meta.primaryTag(),
                List.of(meta == null ? "Implementation" : meta.primaryTag()),
                acceptedAt,
                "ATC:" + contestId + ":" + taskId
        );
        solved.merge(taskId, candidate, (left, right) ->
                left.acceptedAt().isAfter(right.acceptedAt()) ? right : left);
    }

    private Map<String, AtCoderProblemMeta> loadAtCoderProblemMetaMap() {
        Instant now = Instant.now();
        if (!atcProblemMetaMap.isEmpty() && Duration.between(atcProblemMetaLoadedAt, now).toHours() < 6) {
            return atcProblemMetaMap;
        }

        synchronized (atcProblemMetaLock) {
            if (!atcProblemMetaMap.isEmpty() && Duration.between(atcProblemMetaLoadedAt, now).toHours() < 6) {
                return atcProblemMetaMap;
            }

            for (String baseUrl : atcProblemApiBaseUrls) {
                Map<String, AtCoderProblemMeta> loaded = new HashMap<>();
                try {
                    JsonNode problemsRoot = readAtCoderPublicJson(baseUrl + "/problems.json");
                    JsonNode modelsRoot = readAtCoderPublicJson(baseUrl + "/problem-models.json");
                    Map<String, Integer> ratings = parseAtCoderProblemRatings(modelsRoot);
                    for (JsonNode item : problemsRoot) {
                        String taskId = textValue(item, "id");
                        if (taskId == null || taskId.isBlank()) {
                            continue;
                        }
                        loaded.put(taskId.toLowerCase(Locale.ROOT), new AtCoderProblemMeta(
                                firstNonBlank(textValue(item, "title"), taskId),
                                firstNonBlank(textValue(item, "contest_id"), deriveAtCoderContestId(taskId)),
                                ratings.getOrDefault(taskId, 0),
                                "Implementation"
                        ));
                    }
                    atcProblemMetaMap = loaded;
                    atcProblemMetaLoadedAt = now;
                    break;
                } catch (Exception ex) {
                    logger.warn(
                            "AtCoder problem metadata unavailable from source={}, reason={}",
                            baseUrl,
                            ex.getMessage()
                    );
                }
            }

            for (String modelsUrl : ATCODER_PROBLEM_MODEL_FALLBACK_URLS) {
                try {
                    JsonNode modelsRoot = readJson(modelsUrl);
                    Map<String, AtCoderProblemMeta> loaded = buildAtCoderProblemMetaMapFromRatings(
                            parseAtCoderProblemRatings(modelsRoot)
                    );
                    if (!loaded.isEmpty()) {
                        atcProblemMetaMap = loaded;
                        atcProblemMetaLoadedAt = now;
                        logger.info("Loaded AtCoder problem ratings from fallback source={}", modelsUrl);
                        break;
                    }
                } catch (Exception ex) {
                    logger.warn(
                            "AtCoder problem ratings fallback unavailable from source={}, reason={}",
                            modelsUrl,
                            ex.getMessage()
                    );
                }
            }
            return atcProblemMetaMap;
        }
    }

    private Map<String, Integer> parseAtCoderProblemRatings(JsonNode modelsRoot) {
        Map<String, Integer> ratings = new HashMap<>();
        if (modelsRoot == null || !modelsRoot.isObject()) {
            return ratings;
        }
        modelsRoot.fields().forEachRemaining(entry -> {
            JsonNode value = entry.getValue();
            Integer difficulty = integerValue(value, "difficulty");
            if (difficulty != null && difficulty > 0) {
                ratings.put(entry.getKey().toLowerCase(Locale.ROOT), difficulty);
            }
        });
        return ratings;
    }

    private Map<String, AtCoderProblemMeta> buildAtCoderProblemMetaMapFromRatings(Map<String, Integer> ratings) {
        Map<String, AtCoderProblemMeta> loaded = new HashMap<>();
        ratings.forEach((taskId, difficulty) -> loaded.put(
                taskId.toLowerCase(Locale.ROOT),
                new AtCoderProblemMeta(
                        taskId,
                        deriveAtCoderContestId(taskId),
                        difficulty,
                        "Implementation"
                )
        ));
        return loaded;
    }

    private void ensureAtCoderUserExists(String profileHtml) {
        String normalizedHtml = profileHtml == null ? "" : profileHtml;
        if (normalizedHtml.contains("<title>404")
                || normalizedHtml.contains("User not found.")
                || normalizedHtml.contains("404 Page Not Found")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AtCoder 账号不存在或不可访问");
        }
    }

    private int parseAtCoderCurrentRating(String profileHtml) {
        if (profileHtml == null || profileHtml.isBlank()) {
            return 0;
        }
        Matcher matcher = ATCODER_RATING_ROW.matcher(profileHtml);
        if (!matcher.find()) {
            return 0;
        }
        String cellText = matcher.group(1)
                .replaceAll("(?is)<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .trim();
        return parseInteger(cellText);
    }

    private List<ContestHistorySeed> parseAtCoderHistoryJson(String handle) {
        JsonNode root = readJson("https://atcoder.jp/users/" + urlEncode(handle) + "/history/json");
        List<ContestHistorySeed> history = new ArrayList<>();
        for (JsonNode item : root) {
            String contestName = firstNonBlank(item.path("ContestNameEn").asText(""), item.path("ContestName").asText("AtCoder Contest"));
            String contestScreenName = item.path("ContestScreenName").asText("");
            if (contestScreenName.isBlank()) {
                continue;
            }
            history.add(new ContestHistorySeed(
                    "AtCoder",
                    contestName,
                    buildAtCoderContestUrl(contestScreenName),
                    parseAtCoderDate(item.path("EndTime").asText("")),
                    item.path("Place").isMissingNode() ? null : item.path("Place").asInt(),
                    item.path("Performance").isMissingNode() ? null : item.path("Performance").asInt(),
                    item.path("NewRating").isMissingNode() ? null : item.path("NewRating").asInt(),
                    item.path("OldRating").isMissingNode() || item.path("NewRating").isMissingNode()
                            ? null
                            : item.path("NewRating").asInt() - item.path("OldRating").asInt()
            ));
        }
        history.sort(Comparator.comparing(ContestHistorySeed::contestTime).reversed());
        return history.stream().limit(10).toList();
    }

    private LocalDateTime parseAtCoderDate(String value) {
        return OffsetDateTime.parse(value.trim()).atZoneSameInstant(SHANGHAI_ZONE).toLocalDateTime();
    }

    private BigDecimal settlePoints(
            UserAccountEntity user,
            StudentInfoEntity student,
            List<AcceptedProblemSeed> acceptedProblems,
            List<ContestHistorySeed> contestHistory
    ) {
        int hiddenRating = computeHiddenRating(student);
        List<PointLogEntity> newLogs = new ArrayList<>();
        pointLogRepository.deleteByUserIdAndSourceType(user.getId(), "OJ_PROBLEM");
        pointLogRepository.deleteByUserIdAndSourceType(user.getId(), "CONTEST");
        pointLogRepository.flush();
        for (AcceptedProblemSeed problem : acceptedProblems) {
            String sourceKey = "OJ_PROBLEM:" + user.getId() + ":" + problem.platform() + ":" + problem.problemCode();

            PointLogEntity log = new PointLogEntity();
            log.setUserId(user.getId());
            log.setUserName(student.getRealName());
            log.setSourceType("OJ_PROBLEM");
            log.setSourceKey(sourceKey);
            ProblemPointAward award = computeProblemPoints(problem.rating(), hiddenRating);
            log.setReason(buildProblemPointReason(problem, hiddenRating, award));
            log.setPoints(award.points());
            log.setCreatedAt(problem.acceptedAt());
            newLogs.add(log);
        }

        for (ContestHistorySeed contest : contestHistory) {
            String sourceKey = "CONTEST:" + user.getId() + ":" + contest.platform() + ":" + contest.contestUrl();
            PointLogEntity log = new PointLogEntity();
            log.setUserId(user.getId());
            log.setUserName(student.getRealName());
            log.setSourceType("CONTEST");
            log.setSourceKey(sourceKey);
            log.setReason(buildContestPointReason(contest));
            log.setPoints(computeContestPoints(contest));
            log.setCreatedAt(contest.contestTime());
            newLogs.add(log);
        }

        if (!newLogs.isEmpty()) {
            pointLogRepository.saveAll(newLogs);
        }
        BigDecimal totalPoints = pointLogRepository.sumPointsByUserId(user.getId());
        return normalizePoints(totalPoints);
    }

    private String buildProblemPointReason(AcceptedProblemSeed problem, int hiddenRating, ProblemPointAward award) {
        String ratingText = problem.rating() > 0 ? "（" + problem.rating() + "）" : "";
        return "完成 " + problem.platform() + " 题目 " + problem.problemCode() + ratingText
                + "，隐藏分 " + hiddenRating + "，" + award.reason();
    }

    private ProblemPointAward computeProblemPoints(int rating, int hiddenRating) {
        if (rating <= 0) {
            return new ProblemPointAward(BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP), "题目无公开难度，不计分");
        }

        BigDecimal basePoints = BigDecimal.valueOf(rating)
                .movePointLeft(3)
                .setScale(1, RoundingMode.HALF_UP);
        if (rating <= hiddenRating - 400) {
            return new ProblemPointAward(BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP), "低于隐藏分 400+，不计分");
        }
        if (rating <= hiddenRating - 200) {
            BigDecimal discounted = basePoints.multiply(BigDecimal.valueOf(0.5))
                    .setScale(1, RoundingMode.HALF_UP);
            return new ProblemPointAward(discounted, "低于隐藏分 200+，折半计 " + discounted.toPlainString() + " 分");
        }
        return new ProblemPointAward(basePoints, "按题目难度计 " + basePoints.toPlainString() + " 分");
    }

    private String buildContestPointReason(ContestHistorySeed contest) {
        StringBuilder builder = new StringBuilder();
        builder.append(contest.platform()).append(" 比赛 ").append(contest.contestName());
        if (contest.rankNo() != null) {
            builder.append("，排名 ").append(contest.rankNo());
        }
        if (contest.ratingChange() != null && contest.ratingChange() != 0) {
            builder.append("，rating ").append(contest.ratingChange() > 0 ? "+" : "").append(contest.ratingChange());
        }
        return builder.toString();
    }

    private BigDecimal computeContestPoints(ContestHistorySeed contest) {
        int rankPoints;
        int rank = contest.rankNo() == null ? Integer.MAX_VALUE : contest.rankNo();
        if (rank <= 50) {
            rankPoints = 18;
        } else if (rank <= 200) {
            rankPoints = 12;
        } else if (rank <= 500) {
            rankPoints = 8;
        } else if (rank <= 1500) {
            rankPoints = 5;
        } else {
            rankPoints = 3;
        }

        int ratingBonus = contest.ratingChange() == null ? 0 : Math.max(0, contest.ratingChange() / 25);
        return BigDecimal.valueOf(rankPoints + ratingBonus).setScale(1, RoundingMode.HALF_UP);
    }

    private List<AcceptedProblemSeed> loadAcceptedProblemSeeds(Long userId) {
        return ojSolvedProblemRepository.findAllByUserIdOrderByAcceptedAtDesc(userId).stream()
                .map(problem -> new AcceptedProblemSeed(
                        problem.getPlatform(),
                        problem.getProblemCode(),
                        problem.getTitle(),
                        problem.getProblemUrl(),
                        problem.getRating() == null ? 0 : problem.getRating(),
                        problem.getTag() == null || problem.getTag().isBlank() ? "Implementation" : problem.getTag(),
                        List.of(problem.getTag() == null || problem.getTag().isBlank() ? "Implementation" : problem.getTag()),
                        problem.getAcceptedAt(),
                        stripStoredSourceKey(userId, problem.getSourceKey())
                ))
                .toList();
    }

    private List<AcceptedProblemSeed> loadAcceptedProblemSeedsByPlatform(Long userId, String platform) {
        return ojSolvedProblemRepository.findAllByUserIdAndPlatformOrderByAcceptedAtDesc(userId, platform).stream()
                .map(problem -> new AcceptedProblemSeed(
                        problem.getPlatform(),
                        problem.getProblemCode(),
                        problem.getTitle(),
                        problem.getProblemUrl(),
                        problem.getRating() == null ? 0 : problem.getRating(),
                        problem.getTag() == null || problem.getTag().isBlank() ? "Implementation" : problem.getTag(),
                        List.of(problem.getTag() == null || problem.getTag().isBlank() ? "Implementation" : problem.getTag()),
                        problem.getAcceptedAt(),
                        stripStoredSourceKey(userId, problem.getSourceKey())
                ))
                .toList();
    }

    private int computeHiddenRating(StudentInfoEntity student) {
        int cf = student.getCfRating() == null ? 0 : student.getCfRating();
        int atc = student.getAtcRating() == null ? 0 : student.getAtcRating();
        int solved = student.getSolvedCount() == null ? 0 : student.getSolvedCount();

        int baseRating = (int) Math.round(cf * 0.7 + atc * 0.3);
        int expectedSolved = Math.max(60, cf / 12);
        double ratio = expectedSolved == 0 ? 1.0 : solved * 1.0 / expectedSolved;
        int solvedBoost = (int) Math.round((ratio - 1.0) * 220);
        solvedBoost = Math.max(-140, Math.min(320, solvedBoost));
        return Math.max(800, Math.min(3200, baseRating + solvedBoost));
    }

    private BigDecimal normalizePoints(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP) : value.setScale(1, RoundingMode.HALF_UP);
    }

    private void refreshAlerts(StudentInfoEntity student, List<AcceptedProblemSeed> acceptedProblems) {
        LocalDateTime now = LocalDateTime.now(SHANGHAI_ZONE);
        int hiddenRating = computeHiddenRating(student);
        List<AcceptedProblemSeed> recent24Hours = acceptedProblems.stream()
                .filter(item -> !item.acceptedAt().isBefore(now.minusHours(24)))
                .toList();
        List<AcceptedProblemSeed> highJumpProblems = acceptedProblems.stream()
                .filter(item -> !item.acceptedAt().isBefore(now.minusDays(7)))
                .filter(item -> item.rating() >= Math.max(hiddenRating + 400, 1800))
                .toList();

        int highFrequencyThreshold = Math.max(8, 4 + Math.max(hiddenRating, 1200) / 400);
        if (recent24Hours.size() >= highFrequencyThreshold) {
            String problemCodes = recent24Hours.stream()
                    .limit(5)
                    .map(AcceptedProblemSeed::problemCode)
                    .distinct()
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("无");
            persistAlert(
                    student.getUserId(),
                    student.getRealName(),
                    "RULE_1",
                    recent24Hours.size() >= highFrequencyThreshold + 3 ? "HIGH" : "MEDIUM",
                    now,
                    "24 小时内通过 " + recent24Hours.size() + " 题，明显高于当前隐藏分 " + hiddenRating
                            + " 对应的日常训练阈值（约 " + highFrequencyThreshold + " 题）。",
                    problemCodes,
                    "建议核对是否处于集中补题、组队训练或比赛复盘阶段，必要时抽查提交代码。"
            );
        }

        if (highJumpProblems.size() >= 2) {
            String problemCodes = highJumpProblems.stream()
                    .sorted(Comparator.comparing(AcceptedProblemSeed::rating).reversed())
                    .limit(5)
                    .map(item -> item.problemCode() + "(" + item.rating() + ")")
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("无");
            persistAlert(
                    student.getUserId(),
                    student.getRealName(),
                    "RULE_4",
                    highJumpProblems.size() >= 4 ? "HIGH" : "MEDIUM",
                    now,
                    "最近 7 天通过了 " + highJumpProblems.size() + " 道 rating 不低于 "
                            + Math.max(hiddenRating + 400, 1800) + " 的题目，和当前隐藏分段存在明显跳跃。",
                    problemCodes,
                    "建议结合最近比赛记录、训练计划和题目来源进行人工复核，确认是否属于正常拔高训练。"
            );
        }
    }

    private void persistAlert(
            Long userId,
            String userName,
            String ruleCode,
            String riskLevel,
            LocalDateTime hitTime,
            String description,
            String suspiciousProblems,
            String suggestion
    ) {
        AlertLogEntity entity = alertLogRepository.findTop1ByUserNameAndRuleCodeOrderByHitTimeDesc(userName, ruleCode)
                .filter(existing -> !existing.getHitTime().isBefore(hitTime.minusHours(12)))
                .orElseGet(AlertLogEntity::new);

        entity.setUserId(userId);
        entity.setUserName(userName);
        entity.setRuleCode(ruleCode);
        entity.setRiskLevel(riskLevel);
        entity.setHitTime(hitTime);
        entity.setStatus("OPEN");
        entity.setDescription(description);
        entity.setSuspiciousProblems(suspiciousProblems);
        entity.setSuggestion(suggestion);
        alertLogRepository.save(entity);
    }

    private JsonNode readJson(String url) {
        String body = readString(url);
        try {
            return objectMapper.readTree(body);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OJ 数据解析失败");
        }
    }

    private JsonNode readAtCoderPublicJson(String url) {
        waitForAtCoderRequestSlot();
        return readJson(url);
    }

    private String readString(String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .GET()
                .header("User-Agent", "acm-train-sync/1.0")
                .header("Accept", "application/json,text/plain,*/*")
                .timeout(DEFAULT_HTTP_TIMEOUT)
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 404 && url.contains("atcoder.jp/users/")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AtCoder 账号不存在或不可访问");
            }
            if (response.statusCode() >= 400) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OJ 接口请求失败: " + response.statusCode());
            }
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "无法连接到 OJ 站点");
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "无法连接到 OJ 站点");
        }
    }

    private void ensureApiOk(JsonNode root, String fallbackMessage) {
        if (!"OK".equalsIgnoreCase(root.path("status").asText())) {
            String message = root.path("comment").asText(fallbackMessage);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, message);
        }
    }

    private String normalizeRequiredHandle(String handle, String platformName) {
        if (handle == null || handle.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, platformName + " 账号未绑定");
        }
        return handle.trim();
    }

    private JsonNode extractSubmissionArray(JsonNode root) {
        if (root == null || root.isNull()) {
            return null;
        }
        if (root.isArray()) {
            return root;
        }
        if (root.has("submissions")) {
            return root.path("submissions");
        }
        if (root.has("results")) {
            return root.path("results");
        }
        if (root.has("items")) {
            return root.path("items");
        }
        return null;
    }

    private boolean isAcceptedSubmission(JsonNode item) {
        String result = firstNonBlank(
                textValue(item, "result"),
                textValue(item, "status"),
                textValue(item, "verdict")
        );
        return result != null && ("AC".equalsIgnoreCase(result) || "Accepted".equalsIgnoreCase(result));
    }

    private boolean matchesAtCoderUser(JsonNode item, String atcHandle) {
        String user = firstNonBlank(
                textValue(item, "user_id"),
                textValue(item, "userId"),
                textValue(item, "user"),
                textValue(item, "username")
        );
        return user == null || user.isBlank() || atcHandle.equalsIgnoreCase(user.trim());
    }

    private String textValue(JsonNode item, String field) {
        if (item == null || field == null || !item.has(field) || item.path(field).isNull()) {
            return null;
        }
        String value = item.path(field).asText(null);
        return value == null ? null : value.trim();
    }

    private Integer integerValue(JsonNode item, String... fields) {
        for (String field : fields) {
            if (field == null || !item.has(field) || item.path(field).isNull()) {
                continue;
            }
            JsonNode value = item.path(field);
            if (value.isNumber()) {
                return value.asInt();
            }
            String text = value.asText("").trim();
            if (!text.isBlank()) {
                try {
                    return Integer.parseInt(text);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }

    private LocalDateTime parseImportedAcceptedAt(JsonNode item) {
        Integer epochSecond = integerValue(item, "epoch_second", "epochSecond");
        if (epochSecond != null) {
            return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSecond), SHANGHAI_ZONE);
        }

        String text = firstNonBlank(
                textValue(item, "accepted_at"),
                textValue(item, "acceptedAt"),
                textValue(item, "submitted_at"),
                textValue(item, "submittedAt"),
                textValue(item, "created_at"),
                textValue(item, "createdAt")
        );
        if (text == null || text.isBlank()) {
            return LocalDateTime.now(SHANGHAI_ZONE);
        }

        try {
            return OffsetDateTime.parse(text).atZoneSameInstant(SHANGHAI_ZONE).toLocalDateTime();
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(text.replace(" ", "T"));
        } catch (Exception ignored) {
        }
        return LocalDateTime.now(SHANGHAI_ZONE);
    }

    private String deriveAtCoderContestId(String taskId) {
        if (taskId == null || taskId.isBlank()) {
            return null;
        }
        int splitIndex = taskId.lastIndexOf('_');
        if (splitIndex <= 0) {
            return null;
        }
        return taskId.substring(0, splitIndex);
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String buildProblemKey(String contestId, String index) {
        if (contestId == null || contestId.isBlank() || index == null || index.isBlank()) {
            return "";
        }
        return contestId + ":" + index;
    }

    private String buildCodeforcesProblemUrl(String contestId, String index) {
        return "https://codeforces.com/problemset/problem/" + contestId + "/" + index;
    }

    private String buildAtCoderContestUrl(String contestScreenName) {
        String contestId = contestScreenName.split("\\.")[0];
        return "https://atcoder.jp/contests/" + contestId;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String formatCodeforcesProblemCode(String contestId, String index) {
        return "CF " + contestId + index;
    }

    private int parseInteger(String text) {
        String digits = text.replaceAll("[^0-9-]", "");
        if (digits.isBlank() || "-".equals(digits)) {
            return 0;
        }
        return Integer.parseInt(digits);
    }

    private Integer parseOptionalInteger(String text) {
        String digits = text.replaceAll("[^0-9-]", "");
        if (digits.isBlank() || "-".equals(digits)) {
            return null;
        }
        return Integer.parseInt(digits);
    }

    private Integer parseSignedInteger(String text) {
        String normalized = text.replace("−", "-").replaceAll("[^0-9+\\-]", "");
        if (normalized.isBlank() || "-".equals(normalized) || "+".equals(normalized)) {
            return null;
        }
        return Integer.parseInt(normalized);
    }

    private void waitForAtCoderRequestSlot() {
        synchronized (atcRequestLock) {
            long now = System.currentTimeMillis();
            long waitMs = atcRequestIntervalMs - (now - lastAtcRequestAtMs);
            if (waitMs > 0) {
                try {
                    Thread.sleep(waitMs);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AtCoder 公共接口请求被中断");
                }
            }
            lastAtcRequestAtMs = System.currentTimeMillis();
        }
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private List<String> parseBaseUrls(String rawValue, String defaultValue) {
        String candidate = rawValue == null || rawValue.isBlank() ? defaultValue : rawValue;
        List<String> baseUrls = new ArrayList<>();
        for (String part : candidate.split(",")) {
            String normalized = trimTrailingSlash(part);
            if (!normalized.isBlank() && !baseUrls.contains(normalized)) {
                baseUrls.add(normalized);
            }
        }
        if (baseUrls.isEmpty()) {
            baseUrls.add(trimTrailingSlash(defaultValue));
        }
        return List.copyOf(baseUrls);
    }

    private <T> T executeUnderUserLock(Long userId, Supplier<T> action) {
        Object lock = userSyncLocks.computeIfAbsent(userId, ignored -> new Object());
        synchronized (lock) {
            return action.get();
        }
    }

    private boolean isHandleChanged(String currentHandle, String syncedHandle) {
        String normalizedCurrent = normalizeOptionalHandle(currentHandle);
        String normalizedSynced = normalizeOptionalHandle(syncedHandle);
        if (normalizedCurrent == null) {
            return normalizedSynced != null;
        }
        return !normalizedCurrent.equalsIgnoreCase(normalizedSynced);
    }

    private boolean isKnownHandleChanged(String currentHandle, String syncedHandle) {
        String normalizedCurrent = normalizeOptionalHandle(currentHandle);
        String normalizedSynced = normalizeOptionalHandle(syncedHandle);
        if (normalizedCurrent == null) {
            return normalizedSynced != null;
        }
        if (normalizedSynced == null) {
            return false;
        }
        return !normalizedCurrent.equalsIgnoreCase(normalizedSynced);
    }

    private String normalizeOptionalHandle(String handle) {
        if (handle == null || handle.isBlank()) {
            return null;
        }
        return handle.trim();
    }

    private String toStoredSourceKey(Long userId, String sourceKey) {
        return userId + ":" + sourceKey;
    }

    private String stripStoredSourceKey(Long userId, String sourceKey) {
        String prefix = userId + ":";
        if (sourceKey != null && sourceKey.startsWith(prefix)) {
            return sourceKey.substring(prefix.length());
        }
        return sourceKey;
    }

    private StudentInfoEntity findStudentByUserId(Long userId) {
        return studentInfoRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "当前用户暂无学生档案"));
    }

    private UserAccountEntity findUser(Long userId) {
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
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

    private OjContestHistoryResponse toContestHistoryResponse(OjContestHistoryEntity entity) {
        return new OjContestHistoryResponse(
                entity.getId(),
                entity.getPlatform(),
                entity.getContestName(),
                entity.getContestUrl(),
                entity.getContestTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                entity.getRankNo(),
                entity.getPerformance(),
                entity.getNewRating(),
                entity.getRatingChange()
        );
    }

    private record ContestHistorySeed(
            String platform,
            String contestName,
            String contestUrl,
            LocalDateTime contestTime,
            Integer rankNo,
            Integer performance,
            Integer newRating,
            Integer ratingChange
    ) {
    }

    private record CodeforcesProfile(
            int rating,
            int solvedCount,
            List<ContestHistorySeed> history,
            List<AcceptedProblemSeed> acceptedProblems,
            Long latestSubmissionEpochSecond,
            boolean fullRefresh
    ) {
    }

    private record CodeforcesSubmissionBatch(
            List<AcceptedProblemSeed> acceptedProblems,
            Long latestSubmissionEpochSecond
    ) {
    }

    private record AtCoderProfile(
            int rating,
            List<ContestHistorySeed> history,
            List<AcceptedProblemSeed> acceptedProblems,
            Long latestSubmissionEpochSecond,
            boolean acceptedProblemsFetched,
            boolean fullRefresh
    ) {
    }

    private record AtCoderSubmissionBatch(
            List<AcceptedProblemSeed> acceptedProblems,
            Long latestSubmissionEpochSecond,
            boolean fetchedSuccessfully
    ) {
    }

    private record AtCoderProblemMeta(
            String title,
            String contestId,
            int rating,
            String primaryTag
    ) {
    }

    private record SyncedSnapshot(
            StudentInfoEntity student,
            UserAccountEntity user
    ) {
    }

    private record AcceptedProblemSeed(
            String platform,
            String problemCode,
            String title,
            String problemUrl,
            int rating,
            String primaryTag,
            List<String> tags,
            LocalDateTime acceptedAt,
            String sourceKey
    ) {
    }

    private record ProblemPointAward(
            BigDecimal points,
            String reason
    ) {
    }
}
