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
 * Latency healing action.
 *
 * SAFETY:
 *  - Does NOT modify routing tables, firewall rules, or network interfaces.
 *  - Only performs ping-based connectivity retries.
 *
 * Workflow:
 *  1. Read before latency from the metric.
 *  2. Retry ping to device IP for configured retry count.
 *  3. Parse latency from ping output each attempt.
 *  4. If final latency < threshold → RECOVERED.
 *  5. If still high → UNRESOLVED + notify administrator.
 *  6. Record destination, before, after, retry count in HealingResult.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LatencyHealingAction implements HealingAction {

    private final NotificationService notificationService;

    @Value("${healing.latency.threshold:200}")
    private double latencyThreshold;

    @Value("${healing.latency.retry-count:3}")
    private int retryCount;

    @Value("${healing.command-timeout-seconds:5}")
    private long commandTimeoutSeconds;

    @Value("${healing.retry-delay-ms:1000}")
    private long retryDelayMs;

    @Override
    public HealingResult execute(Device device, Metric metric) {

        long startTime    = System.currentTimeMillis();
        String target     = device.getIpAddress();
        double beforeMs   = metric.getLatency();

        HealingResult.HealingResultBuilder result = HealingResult.builder()
                .action("Latency Remediation")
                .beforeValue(String.format("%.1f ms", beforeMs))
                .retryCount(0);

        if (beforeMs < latencyThreshold) {
            return result.success(true)
                    .status(HealingStatus.RECOVERED)
                    .message(String.format("Latency %.1f ms is below threshold %.0f ms. No action needed.", beforeMs, latencyThreshold))
                    .build();
        }

        log.info("[LAT-Heal] Latency {} ms to {} – starting {} retries", beforeMs, target, retryCount);

        double lastLatency = beforeMs;
        int attempts = 0;

        for (int i = 1; i <= retryCount; i++) {
            try {
                Thread.sleep(retryDelayMs);
                double measured = pingLatency(target);
                attempts = i;
                log.info("[LAT-Heal] Retry {} → {} ms", i, measured);

                if (measured >= 0 && measured < latencyThreshold) {
                    lastLatency = measured;
                    String msg = String.format(
                            "Latency RECOVERED on retry %d. Target: %s, Before: %.1f ms, After: %.1f ms.",
                            i, target, beforeMs, measured);
                    log.info("[LAT-Heal] {}", msg);
                    return result.success(true)
                            .status(HealingStatus.RECOVERED)
                            .afterValue(String.format("%.1f ms", measured))
                            .retryCount(attempts)
                            .message(msg)
                            .executionTime(System.currentTimeMillis() - startTime)
                            .build();
                }
                lastLatency = measured >= 0 ? measured : lastLatency;
            } catch (Exception e) {
                log.warn("[LAT-Heal] Retry {} failed: {}", i, e.getMessage());
            }
        }

        // All retries exhausted
        String msg = String.format(
                "Latency UNRESOLVED after %d retries. Target: %s, Before: %.1f ms, After: %.1f ms. Admin intervention required.",
                attempts, target, beforeMs, lastLatency);
        log.warn("[LAT-Heal] {}", msg);
        notificationService.notifyAdministrator("High Latency – Unresolved", msg);

        return result.success(false)
                .status(HealingStatus.UNRESOLVED)
                .afterValue(String.format("%.1f ms", lastLatency))
                .retryCount(attempts)
                .message(msg)
                .executionTime(System.currentTimeMillis() - startTime)
                .build();
    }

    /**
     * Ping a host once and return the average round-trip time in milliseconds.
     * Returns -1 if the ping fails or the output cannot be parsed.
     */
    public double pingLatency(String host) {
        try {
            // ping -c 1 works on both macOS and Linux
            Process p = new ProcessBuilder("ping", "-c", "1", "-W", "3", host)
                    .redirectErrorStream(true).start();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line).append("\n");
            p.waitFor(commandTimeoutSeconds, TimeUnit.SECONDS);

            // Parse "round-trip min/avg/max/stddev = 0.123/0.456/0.789/0.012 ms"
            Matcher m = Pattern.compile("(?:min/avg/max[^=]*=\\s*[\\d.]+/([\\d.]+))")
                    .matcher(sb.toString());
            if (m.find()) return Double.parseDouble(m.group(1));

            // Fallback: "time=0.456 ms"
            Matcher m2 = Pattern.compile("time[<=]([\\d.]+)\\s*ms").matcher(sb.toString());
            if (m2.find()) return Double.parseDouble(m2.group(1));

        } catch (Exception e) {
            log.warn("[LAT-Heal] pingLatency({}) error: {}", host, e.getMessage());
        }
        return -1;
    }
}
