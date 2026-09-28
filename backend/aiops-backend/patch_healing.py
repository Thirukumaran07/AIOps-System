import os

base_dir = "/Users/apple/Project/AIops-network-self-healing/backend/aiops-backend/src/main/java/com/aiops/backend"

# HealingLogRepository.java
healing_log_repository = """package com.aiops.backend.repository;

import com.aiops.backend.entity.HealingLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HealingLogRepository
        extends JpaRepository<HealingLog, Long> {

    List<HealingLog> findByDeviceIdOrderByTimestampDesc(
            Long deviceId
    );

    List<HealingLog> findTop10ByOrderByTimestampDesc();
}
"""

# RecoveryHistory.java
recovery_history = """package com.aiops.backend.healing;

import com.aiops.backend.entity.HealingLog;
import com.aiops.backend.repository.HealingLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecoveryHistory {

    private final HealingLogRepository healingLogRepository;

    public List<HealingLog> getAllHistory() {

        return healingLogRepository
                .findAll()
                .stream()
                .sorted(
                        (a, b) ->
                                b.getTimestamp()
                                        .compareTo(a.getTimestamp())
                )
                .toList();
    }

    public List<HealingLog> getDeviceHistory(
            Long deviceId
    ) {

        return healingLogRepository
                .findByDeviceIdOrderByTimestampDesc(deviceId);
    }

    public List<HealingLog> getRecentHistory() {

        return healingLogRepository
                .findTop10ByOrderByTimestampDesc();
    }
}
"""

# HealingEngine.java
healing_engine = """package com.aiops.backend.healing;

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
                    .actionTaken(healingAction)
                    .status("FAILED")
                    .success(false)
                    .message("No suitable healing action available.")
                    .timestamp(LocalDateTime.now())
                    .build();

            return healingLogRepository.save(log);
        }

        boolean success =
                recoveryExecutor.execute(deviceId, healingAction);

        HealingLog log = HealingLog.builder()
                .deviceId(deviceId)
                .rootCause(rootCause)
                .actionTaken(healingAction)
                .status(success ? "SUCCESS" : "FAILED")
                .success(success)
                .message(
                        success
                                ? "Recovery action executed successfully."
                                : "Recovery action failed."
                )
                .timestamp(LocalDateTime.now())
                .build();

        return healingLogRepository.save(log);
    }
}
"""

with open(f"{base_dir}/repository/HealingLogRepository.java", "w") as f:
    f.write(healing_log_repository)
    
with open(f"{base_dir}/healing/RecoveryHistory.java", "w") as f:
    f.write(recovery_history)
    
with open(f"{base_dir}/healing/HealingEngine.java", "w") as f:
    f.write(healing_engine)

print("Java files patched successfully.")
