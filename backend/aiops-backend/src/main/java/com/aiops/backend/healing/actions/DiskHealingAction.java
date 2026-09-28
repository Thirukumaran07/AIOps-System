package com.aiops.backend.healing.actions;

import com.aiops.backend.entity.Device;
import com.aiops.backend.entity.Metric;
import com.aiops.backend.healing.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Disk healing action.
 *
 * Workflow:
 *  1. Collect current disk usage via {@code df}.
 *  2. List the top-10 largest items in configured safe-paths.
 *  3. Identify .tmp / .log files in the configured whitelist paths.
 *  4. If dry-run=false → delete safe temp/log files only.
 *  5. If dry-run=true  → log what WOULD be deleted.
 *  6. If nothing safe can be cleaned → ADMIN_REVIEW_REQUIRED.
 *  7. Re-check disk usage and record BEFORE / AFTER.
 *
 *  SAFETY: Only files under healing.disk.safe-paths and matching
 *  *.tmp / *.log extensions are ever touched.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DiskHealingAction implements HealingAction {

    private final NotificationService notificationService;

    @Value("${healing.disk.threshold:90}")
    private double diskThreshold;

    @Value("${healing.disk.safe-paths:/tmp}")
    private String safePathsRaw;

    @Value("${healing.dry-run:true}")
    private boolean dryRun;

    @Override
    public HealingResult execute(Device device, Metric metric) {

        long startTime = System.currentTimeMillis();
        List<String> safePaths = Arrays.asList(safePathsRaw.split(","));

        // Capture BEFORE disk usage
        String beforeDisk = readDiskUsage();
        HealingResult.HealingResultBuilder result = HealingResult.builder()
                .action("Disk Remediation")
                .beforeValue(beforeDisk);

        if (metric.getDiskUsage() < diskThreshold) {
            return result.success(true)
                    .status(HealingStatus.RECOVERED)
                    .message("Disk usage " + metric.getDiskUsage() + "% is below threshold. No action needed.")
                    .build();
        }

        // Collect safe files to clean
        List<File> candidates = new ArrayList<>();
        for (String path : safePaths) {
            File dir = new File(path.trim());
            if (!dir.exists() || !dir.isDirectory()) continue;
            File[] files = dir.listFiles((d, name) -> name.endsWith(".tmp") || name.endsWith(".log"));
            if (files != null) candidates.addAll(Arrays.asList(files));
        }

        if (candidates.isEmpty()) {
            String bigItems = topDiskItems();
            String msg = String.format(
                    "Disk usage is %.1f%%. No safe temp/log files found in configured safe-paths. " +
                    "Top disk consumers:%n%s%nADMIN REVIEW REQUIRED before any deletion.",
                    metric.getDiskUsage(), bigItems);
            notificationService.notifyAdministrator("High Disk – Admin Review Required", msg);
            return result.success(false)
                    .status(HealingStatus.ADMIN_REVIEW_REQUIRED)
                    .message(msg)
                    .executionTime(System.currentTimeMillis() - startTime)
                    .build();
        }

        if (dryRun) {
            StringBuilder sb = new StringBuilder("DRY RUN - action would be executed: delete ");
            candidates.forEach(f -> sb.append(f.getAbsolutePath()).append(" "));
            log.info("[DISK-Heal] {}", sb);
            return result.success(true)
                    .status(HealingStatus.DRY_RUN)
                    .message(sb.toString())
                    .executionTime(System.currentTimeMillis() - startTime)
                    .build();
        }

        // Delete safe files
        int deleted = 0;
        for (File f : candidates) {
            if (f.delete()) {
                deleted++;
                log.info("[DISK-Heal] Deleted: {}", f.getAbsolutePath());
            }
        }

        String afterDisk = readDiskUsage();
        String msg = String.format(
                "Deleted %d safe temp/log file(s). Disk BEFORE: %s, AFTER: %s.", deleted, beforeDisk, afterDisk);
        log.info("[DISK-Heal] {}", msg);

        return result.success(true)
                .status(HealingStatus.RECOVERED)
                .afterValue(afterDisk)
                .message(msg)
                .executionTime(System.currentTimeMillis() - startTime)
                .build();
    }

    /** Run {@code df -h} and return a compact summary line. */
    private String readDiskUsage() {
        try {
            Process p = new ProcessBuilder("bash", "-c", "df -h / | tail -1").redirectErrorStream(true).start();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = r.readLine();
            p.waitFor(5, TimeUnit.SECONDS);
            return line != null ? line.trim() : "unknown";
        } catch (Exception e) {
            return "unknown (" + e.getMessage() + ")";
        }
    }

    /** Return the top-10 largest items under / for the admin report. */
    private String topDiskItems() {
        try {
            Process p = new ProcessBuilder("bash", "-c", "du -sh /* 2>/dev/null | sort -rh | head -10")
                    .redirectErrorStream(true).start();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append("  ").append(line).append("\n");
            p.waitFor(10, TimeUnit.SECONDS);
            return sb.toString();
        } catch (Exception e) {
            return "(could not read disk items: " + e.getMessage() + ")";
        }
    }
}
