package com.acmtrain.backend.service;

import com.acmtrain.backend.entity.AlertLogEntity;
import com.acmtrain.backend.entity.UserAccountEntity;
import com.acmtrain.backend.repository.AlertLogRepository;
import com.acmtrain.backend.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Service
public class AlertNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(AlertNotificationService.class);
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AlertLogRepository alertLogRepository;
    private final UserAccountRepository userAccountRepository;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final boolean enabled;
    private final String from;
    private final String recipients;

    public AlertNotificationService(
            AlertLogRepository alertLogRepository,
            UserAccountRepository userAccountRepository,
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${acm.alert.mail.enabled:false}") boolean enabled,
            @Value("${acm.alert.mail.from:no-reply@acmtrain.local}") String from,
            @Value("${acm.alert.mail.to:}") String recipients
    ) {
        this.alertLogRepository = alertLogRepository;
        this.userAccountRepository = userAccountRepository;
        this.mailSenderProvider = mailSenderProvider;
        this.enabled = enabled;
        this.from = from;
        this.recipients = recipients;
    }

    @Scheduled(cron = "${acm.alert.mail.cron:0 */10 * * * *}")
    @Transactional
    public void scheduledSendPendingAlerts() {
        sendPendingAlertsIfEnabled();
    }

    @Transactional
    public void sendPendingAlertsIfEnabled() {
        if (!enabled) {
            return;
        }

        List<String> recipientList = loadRecipients();
        if (recipientList.isEmpty()) {
            logger.warn("Alert mail is enabled but no recipients were configured.");
            return;
        }

        List<AlertLogEntity> pendingAlerts = alertLogRepository.findAllByStatusAndNotifiedAtIsNullOrderByHitTimeAsc("OPEN");
        if (pendingAlerts.isEmpty()) {
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            logger.warn("Alert mail is enabled but JavaMailSender is unavailable.");
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipientList.toArray(String[]::new));
        message.setSubject("[ACM Train] 新异常提醒 " + pendingAlerts.size() + " 条");
        message.setText(buildMailBody(pendingAlerts));

        try {
            mailSender.send(message);
        } catch (Exception ex) {
            logger.warn("Failed to send alert email.", ex);
            return;
        }

        LocalDateTime sentAt = LocalDateTime.now();
        for (AlertLogEntity alert : pendingAlerts) {
            alert.setNotifiedAt(sentAt);
        }
        alertLogRepository.saveAll(pendingAlerts);
        logger.info("Alert email sent successfully, count={}", pendingAlerts.size());
    }

    private List<String> loadRecipients() {
        List<String> dbRecipients = userAccountRepository.findAllByRoleIgnoreCase("coach").stream()
                .map(UserAccountEntity::getEmail)
                .filter(email -> email != null && !email.isBlank())
                .map(String::trim)
                .toList();

        List<String> fallbackRecipients = Arrays.stream(recipients.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
        return java.util.stream.Stream.concat(dbRecipients.stream(), fallbackRecipients.stream())
                .distinct()
                .toList();
    }

    private String buildMailBody(List<AlertLogEntity> alerts) {
        StringBuilder builder = new StringBuilder();
        builder.append("检测到新的训练异常提醒，请及时查看。\n\n");
        for (AlertLogEntity alert : alerts) {
            builder.append("学生：").append(alert.getUserName()).append('\n');
            builder.append("规则：").append(alert.getRuleCode()).append('\n');
            builder.append("风险：").append(alert.getRiskLevel()).append('\n');
            builder.append("触发时间：").append(alert.getHitTime().format(DATETIME_FORMATTER)).append('\n');
            builder.append("说明：").append(alert.getDescription()).append('\n');
            builder.append("可疑题目：").append(alert.getSuspiciousProblems() == null ? "-" : alert.getSuspiciousProblems()).append('\n');
            builder.append("建议：").append(alert.getSuggestion() == null ? "-" : alert.getSuggestion()).append("\n\n");
        }
        builder.append("请登录系统查看完整异常列表。");
        return builder.toString();
    }
}
