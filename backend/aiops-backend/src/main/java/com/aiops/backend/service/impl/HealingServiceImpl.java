package com.aiops.backend.service.impl;

import com.aiops.backend.entity.HealingLog;
import com.aiops.backend.healing.HealingEngine;
import com.aiops.backend.healing.HealingVerificationService;
import com.aiops.backend.service.HealingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class HealingServiceImpl implements HealingService {

    private final HealingEngine healingEngine;
    private final HealingVerificationService verificationService;

    // Stores the last successful healing time for each device
    private final Map<Long, LocalDateTime> lastHealingTime =
            new ConcurrentHashMap<>();

    // Prevent repeated healing within 60 seconds
    private static final long HEALING_COOLDOWN_SECONDS = 60;

    @Override
    public boolean heal(Long deviceId, String rootCause) {

        System.out.println("========== HEALING PROCESS ==========");
        System.out.println("Device ID: " + deviceId);
        System.out.println("Root Cause: " + rootCause);

        // 1. Check healing cooldown
        LocalDateTime lastHealing =
                lastHealingTime.get(deviceId);

        if (lastHealing != null
                && lastHealing
                .plusSeconds(HEALING_COOLDOWN_SECONDS)
                .isAfter(LocalDateTime.now())) {

            System.out.println(
                    "Healing skipped: cooldown active for device "
                            + deviceId
            );

            System.out.println(
                    "Last successful healing: "
                            + lastHealing
            );

            System.out.println(
                    "===================================="
            );

            // Device was already successfully healed recently
            return true;
        }

        // 2. Execute healing action
        HealingLog healingLog =
                healingEngine.heal(deviceId, rootCause);

        System.out.println(
                "Selected Action: "
                        + healingLog.getHealingAction()
        );

        System.out.println(
                "Initial Healing Result: "
                        + (Boolean.TRUE.equals(
                        healingLog.getSuccess())
                        ? "SUCCESS"
                        : "FAILED")
        );

        // 3. If execution itself failed, stop
        if (!Boolean.TRUE.equals(healingLog.getSuccess())) {

            System.out.println(
                    "Healing execution failed."
            );

            System.out.println(
                    "===================================="
            );

            return false;
        }

        // 4. Verify recovery
        boolean recovered =
                verificationService.verifyRecovery(deviceId);

        System.out.println(
                "Recovery Verification: "
                        + (recovered
                        ? "RECOVERED"
                        : "NOT RECOVERED")
        );

        // 5. Store successful healing time
        if (recovered) {

            lastHealingTime.put(
                    deviceId,
                    LocalDateTime.now()
            );

            System.out.println(
                    "Healing cooldown started: "
                            + HEALING_COOLDOWN_SECONDS
                            + " seconds"
            );
        }

        System.out.println(
                "===================================="
        );

        // 6. Return final healing result
        return recovered;
    }
}