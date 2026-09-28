package com.aiops.backend.healing.actions;

import lombok.Builder;
import lombok.Data;

/**
 * Structured result returned by every HealingAction implementation.
 * Replaces loose String returns throughout the healing pipeline.
 */
@Data
@Builder
public class HealingResult {

    /** Whether the healing execution itself succeeded (no exception). */
    private boolean success;

    /** Semantic outcome, e.g. RECOVERED, UNRESOLVED, ADMIN_NOTIFICATION_REQUIRED. */
    private HealingStatus status;

    /** Short label describing what action was taken, e.g. "CPU Remediation". */
    private String action;

    /** Human-readable description of what happened. */
    private String message;

    /** Metric value captured BEFORE the healing action (e.g. "94.5%"). */
    private String beforeValue;

    /** Metric value captured AFTER the healing action (e.g. "55.2%"). */
    private String afterValue;

    /** OS process ID of the terminated/identified process, if applicable. */
    private String processId;

    /** OS process name of the terminated/identified process, if applicable. */
    private String processName;

    /** Number of retries performed (for latency/packet-loss actions). */
    private int retryCount;

    /** Wall-clock time in milliseconds for the healing execution. */
    private Long executionTime;

    /** Exception message or OS error, if applicable. */
    private String errorMessage;
}
