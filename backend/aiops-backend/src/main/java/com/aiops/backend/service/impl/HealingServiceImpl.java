package com.aiops.backend.service.impl;

import com.aiops.backend.entity.Device;
import com.aiops.backend.entity.HealingLog;
import com.aiops.backend.entity.Metric;
import com.aiops.backend.healing.NotificationService;
import com.aiops.backend.healing.actions.CpuHealingAction;
import com.aiops.backend.healing.actions.DiskHealingAction;
import com.aiops.backend.healing.actions.HealingAction;
import com.aiops.backend.healing.actions.HealingResult;
import com.aiops.backend.healing.actions.HealingStatus;
import com.aiops.backend.healing.actions.LatencyHealingAction;
import com.aiops.backend.healing.actions.MemoryHealingAction;
import com.aiops.backend.healing.actions.NetworkHealingAction;
import com.aiops.backend.healing.actions.PacketLossHealingAction;
import com.aiops.backend.repository.DeviceRepository;
import com.aiops.backend.repository.HealingLogRepository;
import com.aiops.backend.repository.MetricRepository;
import com.aiops.backend.service.AlertService;
import com.aiops.backend.service.HealingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Coordinates the self-healing pipeline.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealingServiceImpl implements HealingService {

    private final HealingLogRepository healingLogRepository;
    private final MetricRepository metricRepository;
    private final DeviceRepository deviceRepository;

    private final AlertService alertService;
    private final NotificationService notificationService;

    private final CpuHealingAction cpuHealingAction;
    private final MemoryHealingAction memoryHealingAction;
    private final DiskHealingAction diskHealingAction;
    private final NetworkHealingAction networkHealingAction;
    private final LatencyHealingAction latencyHealingAction;
    private final PacketLossHealingAction packetLossHealingAction;

    @Override
    public boolean heal(Long deviceId, String rootCause) {

        // ---------------------------------------------------------
        // 1. Load device
        // ---------------------------------------------------------

        Device device =
                deviceRepository.findById(deviceId).orElse(null);

        if (device == null) {

            log.error(
                    "[Heal] Device {} not found – cannot perform healing.",
                    deviceId
            );

            return false;
        }

        // ---------------------------------------------------------
        // 2. Load latest metric
        // ---------------------------------------------------------

        Metric metric =
                metricRepository
                        .findByDeviceIdOrderByTimestampDesc(deviceId)
                        .stream()
                        .findFirst()
                        .orElse(null);

        if (metric == null) {

            log.error(
                    "[Heal] No metric found for device {}.",
                    deviceId
            );

            return false;
        }

        // ---------------------------------------------------------
        // 3. Select healing action
        // ---------------------------------------------------------

        HealingAction action =
                selectAction(rootCause, metric);

        if (action == null) {

            log.warn(
                    "[Heal] No healing action mapped for root cause: '{}'",
                    rootCause
            );

            return false;
        }

        // ---------------------------------------------------------
        // 4. Execute healing action
        // ---------------------------------------------------------

        try {

            HealingResult result =
                    action.execute(device, metric);

            if (result == null) {

                log.error(
                        "[Heal] Healing action returned null for device {}.",
                        deviceId
                );

                return false;
            }

            // -----------------------------------------------------
            // 5. Determine final status
            // -----------------------------------------------------

            String status;

            if (result.getStatus() != null) {

                status =
                        result.getStatus().name();

            } else {

                status =
                        result.isSuccess()
                                ? HealingStatus.SUCCESS.name()
                                : HealingStatus.FAILED.name();
            }

            /*
             * A result requiring administrator review must never
             * be represented as successful healing.
             */
            boolean healingSuccessful =
                    result.isSuccess()
                            && !isAdminRequired(result.getStatus());

            // -----------------------------------------------------
            // 6. Persist healing log
            // -----------------------------------------------------

            HealingLog healingLog =
                    HealingLog.builder()
                            .deviceId(deviceId)
                            .metricId(metric.getId())
                            .rootCause(rootCause)
                            .actionTaken(result.getAction())
                            .status(status)
                            .success(healingSuccessful)
                            .beforeValue(result.getBeforeValue())
                            .afterValue(result.getAfterValue())
                            .processId(result.getProcessId())
                            .processName(result.getProcessName())
                            .message(result.getMessage())
                            .errorMessage(result.getErrorMessage())
                            .timestamp(LocalDateTime.now())
                            .build();

            healingLogRepository.save(healingLog);

            log.info(
                    "[Heal] HealingLog saved – status={}, success={}, device={}",
                    status,
                    healingSuccessful,
                    deviceId
            );

            // -----------------------------------------------------
            // 7. Create alert when healing did not resolve issue
            // -----------------------------------------------------

            if (!healingSuccessful) {

                String alertSeverity =
                        isCriticalStatus(result.getStatus())
                                ? "CRITICAL"
                                : "HIGH";

                alertService.createAlert(
                        deviceId,
                        "HEALING_" + status,
                        alertSeverity,
                        "Healing outcome: " + result.getMessage(),
                        metric.getAnomalyScore(),
                        rootCause,
                        "Manual administrator intervention required"
                );
            }

            // -----------------------------------------------------
            // 8. Return actual healing result
            // -----------------------------------------------------

            return healingSuccessful;

        } catch (Exception e) {

            log.error(
                    "[Heal] Unexpected exception during healing "
                            + "for device {}",
                    deviceId,
                    e
            );

            // -----------------------------------------------------
            // Persist failure
            // -----------------------------------------------------

            try {

                healingLogRepository.save(
                        HealingLog.builder()
                                .deviceId(deviceId)
                                .metricId(metric.getId())
                                .rootCause(rootCause)
                                .actionTaken("Attempted automated healing")
                                .status(HealingStatus.FAILED.name())
                                .success(false)
                                .errorMessage(e.getMessage())
                                .timestamp(LocalDateTime.now())
                                .build()
                );

            } catch (Exception logException) {

                log.error(
                        "[Heal] Could not save failure HealingLog",
                        logException
                );
            }

            // -----------------------------------------------------
            // Create alert
            // -----------------------------------------------------

            try {

                alertService.createAlert(
                        deviceId,
                        "HEALING_ERROR",
                        "CRITICAL",
                        "Exception during healing: "
                                + e.getMessage(),
                        metric.getAnomalyScore(),
                        rootCause,
                        "Check backend logs"
                );

            } catch (Exception alertException) {

                log.error(
                        "[Heal] Could not create healing error alert",
                        alertException
                );
            }

            // -----------------------------------------------------
            // Notify administrator
            // -----------------------------------------------------

            try {

                notificationService.notifyAdministrator(
                        "Healing Exception – Device " + deviceId,
                        "Root cause: "
                                + rootCause
                                + "\nException: "
                                + e.getMessage()
                );

            } catch (Exception notificationException) {

                log.error(
                        "[Heal] Could not notify administrator",
                        notificationException
                );
            }

            return false;
        }
    }

    // =============================================================
    // ACTION SELECTION
    // =============================================================

    private HealingAction selectAction(
            String rootCause,
            Metric metric) {

        if (rootCause != null) {

            String rc =
                    rootCause.toLowerCase();

            if (rc.contains("cpu")) {
                return cpuHealingAction;
            }

            if (rc.contains("memory")
                    || rc.contains("mem")) {

                return memoryHealingAction;
            }

            if (rc.contains("disk")
                    || rc.contains("storage")) {

                return diskHealingAction;
            }

            if (rc.contains("packet")
                    || rc.contains("loss")) {

                return packetLossHealingAction;
            }

            if (rc.contains("latency")
                    || rc.contains("network latency")) {

                return latencyHealingAction;
            }

            if (rc.contains("network")) {

                return networkHealingAction;
            }
        }

        // ---------------------------------------------------------
        // Fallback to metric values
        // ---------------------------------------------------------

        if (metric.getCpuUsage() != null
                && metric.getCpuUsage() >= 90) {

            return cpuHealingAction;
        }

        if (metric.getMemoryUsage() != null
                && metric.getMemoryUsage() >= 90) {

            return memoryHealingAction;
        }

        if (metric.getDiskUsage() != null
                && metric.getDiskUsage() >= 90) {

            return diskHealingAction;
        }

        if (metric.getPacketLoss() != null
                && metric.getPacketLoss() >= 5) {

            return packetLossHealingAction;
        }

        if (metric.getLatency() != null
                && metric.getLatency() >= 200) {

            return latencyHealingAction;
        }

        if (metric.getNetworkUsage() != null
                && metric.getNetworkUsage() >= networkThreshold()) {

            return networkHealingAction;
        }

        return null;
    }

    private double networkThreshold() {
        return 90.0;
    }

    // =============================================================
    // STATUS HELPERS
    // =============================================================

    private boolean isAdminRequired(
            HealingStatus status) {

        if (status == null) {
            return false;
        }

        return status ==
                HealingStatus.ADMIN_NOTIFICATION_REQUIRED
                || status ==
                HealingStatus.ADMIN_REVIEW_REQUIRED
                || status ==
                HealingStatus.UNRESOLVED;
    }

    private boolean isCriticalStatus(
            HealingStatus status) {

        return status == HealingStatus.FAILED
                || status == HealingStatus.UNRESOLVED;
    }
}