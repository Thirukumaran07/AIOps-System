package com.aiops.backend.controller;

import com.aiops.backend.entity.HealingLog;
import com.aiops.backend.healing.HealingEngine;
import com.aiops.backend.healing.RecoveryHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/healing")
@RequiredArgsConstructor
public class HealingController {

    private final HealingEngine healingEngine;
    private final RecoveryHistory recoveryHistory;

    /**
     * Manually trigger healing for a device.
     */
    @PostMapping("/execute")
    public HealingLog executeHealing(
            @RequestParam Long deviceId,
            @RequestParam String rootCause
    ) {

        return healingEngine.heal(
                deviceId,
                rootCause
        );
    }

    /**
     * Get all healing history.
     */
    @GetMapping("/history")
    public List<HealingLog> getHistory() {

        return recoveryHistory.getAllHistory();
    }

    /**
     * Get healing history for a device.
     */
    @GetMapping("/history/device/{deviceId}")
    public List<HealingLog> getDeviceHistory(
            @PathVariable Long deviceId
    ) {

        return recoveryHistory.getDeviceHistory(
                deviceId
        );
    }

    /**
     * Get the latest 10 healing operations.
     */
    @GetMapping("/history/recent")
    public List<HealingLog> getRecentHistory() {

        return recoveryHistory.getRecentHistory();
    }
}