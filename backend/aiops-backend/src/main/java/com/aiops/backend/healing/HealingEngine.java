package com.aiops.backend.healing;

import com.aiops.backend.entity.HealingLog;
import com.aiops.backend.repository.HealingLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class HealingEngine {

    private final RecoveryExecutor recoveryExecutor;
    private final HealingLogRepository healingLogRepository;

    /**
     * Determines the healing action based on the root cause.
     */
    public String determineHealingAction(String rootCause) {

        if (rootCause == null || rootCause.isBlank()) {
            return "NO_ACTION";
        }

        return switch (rootCause.toUpperCase()) {

            case "CPU_OVERLOAD" ->
                    "CHECK_HIGH_CPU_PROCESS";

            case "MEMORY_OVERLOAD" ->
                    "CHECK_HIGH_MEMORY_PROCESS";

            case "DISK_OVERLOAD" ->
                    "CHECK_STORAGE_USAGE";

            case "HIGH_NETWORK_LATENCY" ->
                    "CHECK_NETWORK_LATENCY";

            case "HIGH_NETWORK_USAGE" ->
                    "NOTIFY_ADMIN_NETWORK_USAGE";

            case "HIGH_PACKET_LOSS" ->
                    "CHECK_NETWORK_CONNECTIVITY";

            default ->
                    "GENERAL_NETWORK_DIAGNOSTICS";
        };
    }

    /**
     * Executes the self-healing process.
     */
    public HealingLog heal(
            Long deviceId,
            String rootCause
    ) {

        String healingAction =
                determineHealingAction(rootCause);

        if ("NO_ACTION".equals(healingAction)) {

            HealingLog log = HealingLog.builder()
                    .deviceId(deviceId)
                    .rootCause(rootCause)
                    .healingAction(healingAction)
                    .success(false)
                    .details("No suitable healing action available.")
                    .executedAt(LocalDateTime.now())
                    .build();

            return healingLogRepository.save(log);
        }

        boolean success =
                recoveryExecutor.execute(deviceId, healingAction);

        HealingLog log = HealingLog.builder()
                .deviceId(deviceId)
                .rootCause(rootCause)
                .healingAction(healingAction)
                .success(success)
                .details(
                        success
                                ? "Recovery action executed successfully."
                                : "Recovery action failed."
                )
                .executedAt(LocalDateTime.now())
                .build();

        return healingLogRepository.save(log);
    }
}