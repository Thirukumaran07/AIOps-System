package com.aiops.backend.healing;

/**
 * Abstraction for notifying the administrator about unresolved anomalies or
 * healing failures.  The initial implementation logs to the application log;
 * it can later be wired to email, Slack, Teams, etc. without changing callers.
 */
public interface NotificationService {

    /**
     * Notify the administrator with the supplied message.
     *
     * @param subject  short one-line subject / event type
     * @param message  full human-readable description
     */
    void notifyAdministrator(String subject, String message);
}
