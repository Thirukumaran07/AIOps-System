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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * CPU healing action.
 *
 * Workflow:
 *  1. Identify the top-CPU process via {@code ps}.
 *  2. Classify it as critical or non-critical using the configured safe-process list.
 *  3. If non-critical and dry-run=false → terminate → wait → re-check CPU.
 *  4. If critical → ADMIN_NOTIFICATION_REQUIRED.
 *  5. If dry-run=true → log intent only.
 *  6. Record BEFORE / AFTER CPU values in HealingResult.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CpuHealingAction implements HealingAction {

    private final NotificationService notificationService;

    @Value("${healing.cpu.threshold:90}")
    private double cpuThreshold;

    /** Comma-separated process names that must NEVER be terminated. */
    @Value("${healing.cpu.safe-processes:java,postgres,postgresql,python,sshd,launchd,kernel_task}")
    private String safeProcessesRaw;

    @Value("${healing.dry-run:true}")
    private boolean dryRun;

    /** Milliseconds to wait after termination before re-reading CPU. */
    @Value("${healing.retry-delay-ms:2000}")
    private long retryDelayMs;

    @Override
    public HealingResult execute(Device device, Metric metric) {

        long startTime = System.currentTimeMillis();
        List<String> criticalProcesses = Arrays.asList(safeProcessesRaw.split(","));

        HealingResult.HealingResultBuilder result = HealingResult.builder()
                .action("CPU Remediation")
                .beforeValue(String.format("%.1f%%", metric.getCpuUsage()));

        if (metric.getCpuUsage() < cpuThreshold) {
            return result.success(true)
                    .status(HealingStatus.RECOVERED)
                    .message("CPU usage " + metric.getCpuUsage() + "% is below threshold " + cpuThreshold + "%. No action needed.")
                    .build();
        }

        try {
            // Step 1 – Find the top CPU-consuming process
            String[] cmd = {"bash", "-c", "ps -eo pid,pcpu,comm --sort=-pcpu | awk 'NR==2{print $1,$2,$3}'"};
            Process ps = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(ps.getInputStream()));
            String line = reader.readLine();
            ps.waitFor(5, TimeUnit.SECONDS);

            if (line == null || line.trim().isEmpty()) {
                return result.success(false)
                        .status(HealingStatus.FAILED)
                        .message("Could not read process list from OS.")
                        .build();
            }

            String[] parts = line.trim().split("\\s+");
            if (parts.length < 3) {
                return result.success(false)
                        .status(HealingStatus.FAILED)
                        .message("Unexpected ps output format: " + line)
                        .build();
            }

            String pid      = parts[0];
            String pcpu     = parts[1];
            String procName = parts[2];

            result.processId(pid).processName(procName).beforeValue(pcpu + "% CPU");
            log.info("[CPU-Heal] Top CPU process: {} (PID {}) = {}%", procName, pid, pcpu);

            // Step 2 – Classify the process
            boolean isCritical = criticalProcesses.stream()
                    .map(String::trim)
                    .anyMatch(c -> procName.toLowerCase().contains(c.toLowerCase()));

            if (isCritical) {
                String msg = String.format(
                        "ADMIN ALERT: CPU usage is %.1f%%. High-CPU process '%s' (PID %s) is CRITICAL and was NOT terminated automatically.",
                        metric.getCpuUsage(), procName, pid);
                notificationService.notifyAdministrator("High CPU – Critical Process", msg);
                return result.success(false)
                        .status(HealingStatus.ADMIN_NOTIFICATION_REQUIRED)
                        .message(msg)
                        .executionTime(System.currentTimeMillis() - startTime)
                        .build();
            }

            // Step 3 – Non-critical: terminate or dry-run
            if (dryRun) {
                String msg = String.format(
                        "DRY RUN - action would be executed: kill -15 %s (%s, %.1f%% CPU).",
                        pid, procName, metric.getCpuUsage());
                log.info("[CPU-Heal] {}", msg);
                return result.success(true)
                        .status(HealingStatus.DRY_RUN)
                        .message(msg)
                        .executionTime(System.currentTimeMillis() - startTime)
                        .build();
            }

            // Actual termination
            Process kill = new ProcessBuilder("kill", "-15", pid).start();
            kill.waitFor(5, TimeUnit.SECONDS);

            // Step 4 – Wait then re-check CPU
            Thread.sleep(retryDelayMs);

            String[] cpuCheck = {"bash", "-c", "ps -A -o pcpu | awk '{sum+=$1} END {print sum}'"};
            Process cpuProc = new ProcessBuilder(cpuCheck).redirectErrorStream(true).start();
            BufferedReader cpuReader = new BufferedReader(new InputStreamReader(cpuProc.getInputStream()));
            String afterCpuLine = cpuReader.readLine();
            cpuProc.waitFor(5, TimeUnit.SECONDS);

            String afterValue = (afterCpuLine != null) ? afterCpuLine.trim() + "% CPU" : "unknown";
            result.afterValue(afterValue);

            double afterCpu = 0;
            try { afterCpu = Double.parseDouble(afterCpuLine != null ? afterCpuLine.trim() : "0"); } catch (NumberFormatException ignored) {}

            if (afterCpu < cpuThreshold) {
                String msg = String.format("Terminated non-critical process '%s' (PID %s, was %s%% CPU). CPU after recovery: %.1f%%.", procName, pid, pcpu, afterCpu);
                log.info("[CPU-Heal] RECOVERED: {}", msg);
                return result.success(true)
                        .status(HealingStatus.RECOVERED)
                        .message(msg)
                        .executionTime(System.currentTimeMillis() - startTime)
                        .build();
            } else {
                String msg = String.format("Terminated '%s' (PID %s) but CPU is still %.1f%%. Manual intervention required.", procName, pid, afterCpu);
                notificationService.notifyAdministrator("High CPU – Healing Unresolved", msg);
                return result.success(false)
                        .status(HealingStatus.UNRESOLVED)
                        .message(msg)
                        .executionTime(System.currentTimeMillis() - startTime)
                        .build();
            }

        } catch (Exception e) {
            log.error("[CPU-Heal] Exception during remediation", e);
            return result.success(false)
                    .status(HealingStatus.FAILED)
                    .errorMessage(e.getMessage())
                    .message("CPU remediation threw an exception: " + e.getMessage())
                    .executionTime(System.currentTimeMillis() - startTime)
                    .build();
        }
    }
}
