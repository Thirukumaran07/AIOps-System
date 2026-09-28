package com.aiops.backend.healing.actions;

import com.aiops.backend.entity.Device;
import com.aiops.backend.entity.Metric;
import com.aiops.backend.healing.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Packet-loss healing action.
 *
 * SAFETY:
 *  - Does NOT modify routing tables, firewall rules, or disable interfaces.
 *  - Performs connectivity retries using ping only.
 *
 * Workflow:
 *  1. Read before packet-loss from the metric.
 *  2. Retry ping to device IP for configured retry count.
 *  3. Parse packet loss % from ping output each attempt.
 *  4. If packet loss drops below threshold → RECOVERED.
 *  5. If still high → UNRESOLVED + notify administrator.
 *  6. Record destination, before/after loss, retry count in HealingResult.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PacketLossHealingAction implements HealingAction {

    private final NotificationService notificationService;

    @Value("${healing.packet-loss.threshold:5}")
    private double packetLossThreshold;

    @Value("${healing.packet-loss.retry-count:3}")
    private int retryCount;

    @Value("${healing.command-timeout-seconds:5}")
    private long commandTimeoutSeconds;

    @Value("${healing.retry-delay-ms:1000}")
    private long retryDelayMs;

    @Override
    public HealingResult execute(Device device, Metric metric) {

        long startTime  = System.currentTimeMillis();
        String target   = device.getIpAddress();
        double beforePL = metric.getPacketLoss();

        HealingResult.HealingResultBuilder result = HealingResult.builder()
                .action("Packet-Loss Remediation")
                .beforeValue(String.format("%.1f%%", beforePL))
                .retryCount(0);

        if (beforePL < packetLossThreshold) {
            return result.success(true)
                    .status(HealingStatus.RECOVERED)
                    .message(String.format("Packet loss %.1f%% is below threshold %.0f%%. No action needed.", beforePL, packetLossThreshold))
                    .build();
        }

        log.info("[PKT-Heal] Packet loss {}% to {} – starting {} retries", beforePL, target, retryCount);

        double lastLoss = beforePL;
        int attempts    = 0;

        for (int i = 1; i <= retryCount; i++) {
            try {
                Thread.sleep(retryDelayMs);
                double measured = measurePacketLoss(target);
                attempts = i;
                log.info("[PKT-Heal] Retry {} → {}% loss", i, measured);

                if (measured >= 0 && measured < packetLossThreshold) {
                    lastLoss = measured;
                    String msg = String.format(
                            "Packet loss RECOVERED on retry %d. Target: %s, Before: %.1f%%, After: %.1f%%.",
                            i, target, beforePL, measured);
                    log.info("[PKT-Heal] {}", msg);
                    return result.success(true)
                            .status(HealingStatus.RECOVERED)
                            .afterValue(String.format("%.1f%%", measured))
                            .retryCount(attempts)
                            .message(msg)
                            .executionTime(System.currentTimeMillis() - startTime)
                            .build();
                }
                lastLoss = measured >= 0 ? measured : lastLoss;
            } catch (Exception e) {
                log.warn("[PKT-Heal] Retry {} failed: {}", i, e.getMessage());
            }
        }

        // All retries exhausted
        String msg = String.format(
                "Packet loss UNRESOLVED after %d retries. Target: %s, Before: %.1f%%, After: %.1f%%. Admin intervention required.",
                attempts, target, beforePL, lastLoss);
        log.warn("[PKT-Heal] {}", msg);
        notificationService.notifyAdministrator("High Packet Loss – Unresolved", msg);

        return result.success(false)
                .status(HealingStatus.UNRESOLVED)
                .afterValue(String.format("%.1f%%", lastLoss))
                .retryCount(attempts)
                .message(msg)
                .executionTime(System.currentTimeMillis() - startTime)
                .build();
    }

    /**
     * Measure packet loss to the given host using a multi-packet ping.
     * Returns packet-loss percentage (0–100), or -1 on error.
     */
    public double measurePacketLoss(String host) {
        try {
            // Send 10 pings so the % is meaningful
            Process p = new ProcessBuilder("ping", "-c", "10", "-W", "3", host)
                    .redirectErrorStream(true).start();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line).append("\n");
            p.waitFor(commandTimeoutSeconds * 2, TimeUnit.SECONDS); // 10 pings take longer

            // Match "X% packet loss"
            Matcher m = Pattern.compile("(\\d+(?:\\.\\d+)?)%\\s+packet loss").matcher(sb.toString());
            if (m.find()) return Double.parseDouble(m.group(1));

        } catch (Exception e) {
            log.warn("[PKT-Heal] measurePacketLoss({}) error: {}", host, e.getMessage());
        }
        return -1;
    }
}
