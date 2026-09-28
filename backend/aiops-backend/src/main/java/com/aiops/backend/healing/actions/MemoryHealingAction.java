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
 * Memory healing action.
 *
 * Workflow:
 *  1. Identify the top-memory-consuming process via {@code ps}.
 *  2. If critical → ADMIN_NOTIFICATION_REQUIRED.
 *  3. If non-critical and dry-run=false → terminate → re-check memory.
 *  4. Compare BEFORE / AFTER memory usage.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemoryHealingAction implements HealingAction {

    private final NotificationService notificationService;

    @Value("${healing.memory.threshold:90}")
    private double memoryThreshold;

    @Value("${healing.memory.safe-processes:java,postgres,postgresql,python,sshd,launchd,kernel_task}")
    private String criticalProcessesRaw;

    @Value("${healing.dry-run:true}")
    private boolean dryRun;

    @Value("${healing.retry-delay-ms:2000}")
    private long retryDelayMs;

    @Override
    public HealingResult execute(Device device, Metric metric) {

        long startTime = System.currentTimeMillis();
        List<String> criticalProcesses = Arrays.asList(criticalProcessesRaw.split(","));

        HealingResult.HealingResultBuilder result = HealingResult.builder()
                .action("Memory Remediation")
                .beforeValue(String.format("%.1f%%", metric.getMemoryUsage()));

        if (metric.getMemoryUsage() < memoryThreshold) {
            return result.success(true)
                    .status(HealingStatus.RECOVERED)
                    .message("Memory usage " + metric.getMemoryUsage() + "% is below threshold. No action needed.")
                    .build();
        }

        try {
            // Step 1 – Find top memory process
            String[] cmd = {"bash", "-c", "ps -eo pid,pmem,comm --sort=-pmem | awk 'NR==2{print $1,$2,$3}'"};
            Process ps = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(ps.getInputStream()));
            String line = reader.readLine();
            ps.waitFor(5, TimeUnit.SECONDS);

            if (line == null || line.trim().isEmpty()) {
                return result.success(false).status(HealingStatus.FAILED)
                        .message("Could not read process list from OS.").build();
            }

            String[] parts = line.trim().split("\\s+");
            if (parts.length < 3) {
                return result.success(false).status(HealingStatus.FAILED)
                        .message("Unexpected ps output: " + line).build();
            }

            String pid      = parts[0];
            String pmem     = parts[1];
            String procName = parts[2];

            result.processId(pid).processName(procName).beforeValue(pmem + "% MEM");
            log.info("[MEM-Heal] Top memory process: {} (PID {}) = {}% MEM", procName, pid, pmem);

            // Step 2 – Classify
            boolean isCritical = criticalProcesses.stream()
                    .map(String::trim)
                    .anyMatch(c -> procName.toLowerCase().contains(c.toLowerCase()));

            if (isCritical) {
                String msg = String.format(
                        "ADMIN ALERT: Memory usage is %.1f%%. High-memory process '%s' (PID %s) is CRITICAL – NOT terminated. Manual intervention required.",
                        metric.getMemoryUsage(), procName, pid);
                notificationService.notifyAdministrator("High Memory – Critical Process", msg);
                return result.success(false)
                        .status(HealingStatus.ADMIN_NOTIFICATION_REQUIRED)
                        .message(msg)
                        .executionTime(System.currentTimeMillis() - startTime)
                        .build();
            }

            // Step 3 – Non-critical
            if (dryRun) {
                String msg = String.format(
                        "DRY RUN - action would be executed: kill -15 %s (%s, %s%% MEM).", pid, procName, pmem);
                log.info("[MEM-Heal] {}", msg);
                return result.success(true)
                        .status(HealingStatus.DRY_RUN)
                        .message(msg)
                        .executionTime(System.currentTimeMillis() - startTime)
                        .build();
            }

            new ProcessBuilder("kill", "-15", pid).start().waitFor(5, TimeUnit.SECONDS);
            Thread.sleep(retryDelayMs);

            // Step 4 – Re-check memory (approximation via free command on Linux; vm_stat on macOS)
            String[] memCheck = {"bash", "-c",
                    "vm_stat 2>/dev/null | awk '/Pages free/{free=$3} /Pages active/{active=$3} /Pages wired/{wired=$3} END{print (active+wired)*4096}' " +
                    "|| free -b 2>/dev/null | awk '/Mem:/{print $3/$2*100}'"};
            Process memProc = new ProcessBuilder(memCheck).redirectErrorStream(true).start();
            BufferedReader memReader = new BufferedReader(new InputStreamReader(memProc.getInputStream()));
            String afterMemLine = memReader.readLine();
            memProc.waitFor(5, TimeUnit.SECONDS);

            String afterValue = (afterMemLine != null && !afterMemLine.trim().isEmpty())
                    ? afterMemLine.trim() + "% MEM" : "unknown";
            result.afterValue(afterValue);

            String msg = String.format(
                    "Terminated non-critical process '%s' (PID %s, was %s%% MEM). Memory after recovery: %s.",
                    procName, pid, pmem, afterValue);
            log.info("[MEM-Heal] {}", msg);
            return result.success(true)
                    .status(HealingStatus.RECOVERED)
                    .message(msg)
                    .executionTime(System.currentTimeMillis() - startTime)
                    .build();

        } catch (Exception e) {
            log.error("[MEM-Heal] Exception during remediation", e);
            return result.success(false)
                    .status(HealingStatus.FAILED)
                    .errorMessage(e.getMessage())
                    .message("Memory remediation threw an exception: " + e.getMessage())
                    .executionTime(System.currentTimeMillis() - startTime)
                    .build();
        }
    }
}
