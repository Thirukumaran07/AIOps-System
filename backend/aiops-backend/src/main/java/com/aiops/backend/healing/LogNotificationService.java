package com.aiops.backend.healing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Log-based administrator notification.
 * Replace / extend this class to add email, Slack, Teams, etc.
 */
@Slf4j
@Service
public class LogNotificationService implements NotificationService {

    @Override
    public void notifyAdministrator(String subject, String message) {
        log.warn("========================================");
        log.warn("  ADMIN ALERT: {}", subject);
        log.warn("  {}", message);
        log.warn("========================================");
    }
}
