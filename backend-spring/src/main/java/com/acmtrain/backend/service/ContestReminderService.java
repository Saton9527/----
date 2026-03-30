package com.acmtrain.backend.service;

import com.acmtrain.backend.dto.ContestResponse;
import com.acmtrain.backend.entity.ContestLinkEntity;
import com.acmtrain.backend.entity.UserAccountEntity;
import com.acmtrain.backend.repository.ContestLinkRepository;
import com.acmtrain.backend.repository.UserAccountRepository;
import com.acmtrain.backend.service.dto.DtoMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class ContestReminderService {

    private static final Logger logger = LoggerFactory.getLogger(ContestReminderService.class);
    private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter MAIL_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter ATCODER_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssZ");

    private final ContestLinkRepository contestLinkRepository;
    private final UserAccountRepository userAccountRepository;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final HttpClient httpClient;
    private final boolean syncEnabled;
    private final boolean reminderEnabled;
    private final int codeforcesReminderMinutes;
    private final int atCoderReminderMinutes;
    private final String mailFrom;
    private final String mailRecipients;

    public ContestReminderService(
            ContestLinkRepository contestLinkRepository,
            UserAccountRepository userAccountRepository,
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${acm.contest.sync-enabled:false}") boolean syncEnabled,
            @Value("${acm.contest.reminder-enabled:false}") boolean reminderEnabled,
            @Value("${acm.contest.cf-reminder-minutes:120}") int codeforcesReminderMinutes,
            @Value("${acm.contest.atc-reminder-minutes:120}") int atCoderReminderMinutes,
            @Value("${acm.contest.mail.from:no-reply@acmtrain.local}") String mailFrom,
            @Value("${acm.contest.mail.to:}") String mailRecipients
    ) {
        this.contestLinkRepository = contestLinkRepository;
        this.userAccountRepository = userAccountRepository;
        this.mailSenderProvider = mailSenderProvider;
        this.syncEnabled = syncEnabled;
        this.reminderEnabled = reminderEnabled;
        this.codeforcesReminderMinutes = codeforcesReminderMinutes;
        this.atCoderReminderMinutes = atCoderReminderMinutes;
        this.mailFrom = mailFrom;
        this.mailRecipients = mailRecipients;
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Scheduled(cron = "${acm.contest.sync-cron:0 0 */4 * * *}")
    @Transactional
    public void scheduledSyncOfficialContests() {
        if (!syncEnabled) {
            return;
        }
        syncOfficialContests();
    }

    @Scheduled(cron = "${acm.contest.reminder-cron:0 */10 * * * *}")
    @Transactional
    public void scheduledSendContestReminders() {
        sendContestRemindersIfEnabled();
    }

    @Transactional
    public List<ContestResponse> syncOfficialContestsNow(Long operatorId) {
        UserAccountEntity operator = userAccountRepository.findById(operatorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "当前用户不存在"));
        if (!"coach".equalsIgnoreCase(operator.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有教练可以同步官方比赛");
        }
        syncOfficialContests();
        return contestLinkRepository.findAllByOrderByStartTimeAscIdAsc().stream()
                .map(DtoMapper::toContestResponse)
                .toList();
    }

    @Transactional
    public void sendContestRemindersIfEnabled() {
        if (!reminderEnabled) {
            return;
        }

        List<String> recipients = loadRecipients();
        if (recipients.isEmpty()) {
            logger.warn("Contest reminder is enabled but no recipients were configured.");
            return;
        }

        List<ContestLinkEntity> dueContests = contestLinkRepository.findAllByRemindedAtIsNullAndStartTimeAfterOrderByStartTimeAscIdAsc(
                LocalDateTime.now(SHANGHAI_ZONE)
        ).stream()
                .filter(item -> !item.getStartTime().minusMinutes(item.getReminderMinutes()).isAfter(LocalDateTime.now(SHANGHAI_ZONE)))
                .limit(10)
                .toList();
        if (dueContests.isEmpty()) {
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            logger.warn("Contest reminder is enabled but JavaMailSender is unavailable.");
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(recipients.toArray(String[]::new));
        message.setSubject("[ACM Train] 即将开始的比赛提醒 " + dueContests.size() + " 场");
        message.setText(buildMailBody(dueContests));

        try {
            mailSender.send(message);
        } catch (Exception ex) {
            logger.warn("Failed to send contest reminder email.", ex);
            return;
        }

        LocalDateTime remindedAt = LocalDateTime.now(SHANGHAI_ZONE);
        for (ContestLinkEntity contest : dueContests) {
            contest.setRemindedAt(remindedAt);
        }
        contestLinkRepository.saveAll(dueContests);
        logger.info("Contest reminder email sent successfully, count={}", dueContests.size());
    }

    private void syncOfficialContests() {
        List<ContestSeed> officialContests = new ArrayList<>();
        officialContests.addAll(fetchUpcomingCodeforcesContests());
        officialContests.addAll(fetchUpcomingAtCoderContests());

        int createdOrUpdated = 0;
        for (ContestSeed seed : officialContests) {
            ContestLinkEntity entity = contestLinkRepository.findBySourceKey(seed.sourceKey())
                    .orElseGet(ContestLinkEntity::new);
            entity.setPlatform(seed.platform());
            entity.setSourceType("OFFICIAL");
            entity.setSourceKey(seed.sourceKey());
            entity.setTitle(seed.title());
            entity.setUrl(seed.url());
            entity.setStartTime(seed.startTime());
            entity.setReminderMinutes(seed.reminderMinutes());
            entity.setCreatedBy(0L);
            if (entity.getCreatedAt() == null) {
                entity.setCreatedAt(LocalDateTime.now(SHANGHAI_ZONE));
            }
            if (entity.getRemindedAt() != null && entity.getStartTime().isAfter(LocalDateTime.now(SHANGHAI_ZONE))) {
                entity.setRemindedAt(null);
            }
            contestLinkRepository.save(entity);
            createdOrUpdated++;
        }
        logger.info("Official contest sync finished, upserted={}", createdOrUpdated);
    }

    private List<ContestSeed> fetchUpcomingCodeforcesContests() {
        String body = readString("https://codeforces.com/api/contest.list?gym=false");
        try {
            com.fasterxml.jackson.databind.JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(body);
            if (!"OK".equalsIgnoreCase(root.path("status").asText())) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Codeforces 比赛接口返回异常");
            }

            List<ContestSeed> contests = new ArrayList<>();
            for (com.fasterxml.jackson.databind.JsonNode item : root.path("result")) {
                if (!"BEFORE".equalsIgnoreCase(item.path("phase").asText())) {
                    continue;
                }
                long contestId = item.path("id").asLong();
                long startTimeSeconds = item.path("startTimeSeconds").asLong(0L);
                if (contestId <= 0 || startTimeSeconds <= 0) {
                    continue;
                }
                contests.add(new ContestSeed(
                        "Codeforces",
                        item.path("name").asText("Codeforces Contest"),
                        "https://codeforces.com/contest/" + contestId,
                        LocalDateTime.ofInstant(Instant.ofEpochSecond(startTimeSeconds), SHANGHAI_ZONE),
                        "CF:" + contestId,
                        codeforcesReminderMinutes
                ));
                if (contests.size() >= 8) {
                    break;
                }
            }
            return contests;
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Codeforces 比赛接口解析失败");
        }
    }

    private List<ContestSeed> fetchUpcomingAtCoderContests() {
        Document document = Jsoup.parse(readString("https://atcoder.jp/contests/"));
        Element section = document.getElementById("contest-table-upcoming");
        if (section == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AtCoder 比赛页结构发生变化");
        }

        List<ContestSeed> contests = new ArrayList<>();
        for (Element row : section.select("tbody tr")) {
            Element timeElement = row.selectFirst("time.fixtime");
            Element contestLink = row.selectFirst("td:nth-of-type(2) a[href^=/contests/]");
            if (timeElement == null || contestLink == null) {
                continue;
            }

            String isoTime = timeElement.text().trim();
            String contestPath = contestLink.attr("href").trim();
            String contestId = contestPath.substring(contestPath.lastIndexOf('/') + 1);
            if (contestId.isBlank()) {
                continue;
            }

            contests.add(new ContestSeed(
                    "AtCoder",
                    contestLink.text().trim(),
                    "https://atcoder.jp" + contestPath,
                    OffsetDateTime.parse(isoTime, ATCODER_TIME).atZoneSameInstant(SHANGHAI_ZONE).toLocalDateTime(),
                    "ATC:" + contestId,
                    atCoderReminderMinutes
            ));
            if (contests.size() >= 8) {
                break;
            }
        }
        return contests;
    }

    private List<String> loadRecipients() {
        List<String> dbRecipients = userAccountRepository.findAllByRoleIgnoreCase("coach").stream()
                .map(UserAccountEntity::getEmail)
                .filter(email -> email != null && !email.isBlank())
                .map(String::trim)
                .toList();
        List<String> fallbackRecipients = Arrays.stream(mailRecipients.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
        return java.util.stream.Stream.concat(dbRecipients.stream(), fallbackRecipients.stream())
                .distinct()
                .toList();
    }

    private String buildMailBody(List<ContestLinkEntity> contests) {
        StringBuilder builder = new StringBuilder();
        builder.append("以下比赛已到提醒时间，请及时关注。\n\n");
        for (ContestLinkEntity contest : contests) {
            builder.append("平台：").append(contest.getPlatform()).append('\n');
            builder.append("名称：").append(contest.getTitle()).append('\n');
            builder.append("开始时间：").append(contest.getStartTime().format(MAIL_TIME)).append('\n');
            builder.append("提醒规则：提前 ").append(contest.getReminderMinutes()).append(" 分钟\n");
            builder.append("链接：").append(contest.getUrl()).append("\n\n");
        }
        builder.append("请登录系统查看完整比赛提醒列表。");
        return builder.toString();
    }

    private String readString(String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .GET()
                .header("User-Agent", "acm-train-contest-sync/1.0")
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 400) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "官方比赛接口请求失败: " + response.statusCode());
            }
            return response.body();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "官方比赛接口请求被中断");
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "无法连接到官方比赛接口");
        }
    }

    private record ContestSeed(
            String platform,
            String title,
            String url,
            LocalDateTime startTime,
            String sourceKey,
            int reminderMinutes
    ) {
    }
}
