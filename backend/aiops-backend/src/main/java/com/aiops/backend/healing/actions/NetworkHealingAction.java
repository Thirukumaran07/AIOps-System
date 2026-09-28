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
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Network usage healing action.
 *
 * Safety policy:
 *  - Does NOT disable network interfaces.
 *  - Does NOT modify routing tables.
 *  - Does NOT modify firewall rules.
 *  - Does NOT terminate protected/critical processes.
 *  - Performs diagnostic analysis.
 *  - Automatically remediates only explicitly allowed non-critical processes.
 *  - Verifies the condition after remediation.
 *  - Requests administrator intervention when safe remediation is not possible.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NetworkHealingAction implements HealingAction {

    private final NotificationService notificationService;

    @Value("${healing.network.threshold:90}")
    private double networkThreshold;

    @Value("${healing.command-timeout-seconds:5}")
    private long commandTimeoutSeconds;

    /**
     * Automatic process remediation is disabled by default.
     *
     * Enable only after explicitly configuring which processes are safe
     * to terminate.
     */
    @Value("${healing.network.process-remediation-enabled:false}")
    private boolean processRemediationEnabled;

    /**
     * Processes that must never be terminated automatically.
     */
    private static final Set<String> PROTECTED_PROCESSES = Set.of(
            "java",
            "java.exe",
            "postgres",
            "postgresql",
            "postgres.exe",
            "mongod",
            "mongod.exe",
            "mysqld",
            "mysqld.exe",
            "systemd",
            "kernel_task",
            "init",
            "launchd",
            "sshd",
            "networkmanager",
            "NetworkManager"
    );

    @Override
    public HealingResult execute(Device device, Metric metric) {

        long startTime = System.currentTimeMillis();

        Double networkUsageValue = metric.getNetworkUsage();

        if (networkUsageValue == null) {

            String message =
                    "Network healing could not be performed because the "
                            + "network utilization value is unavailable.";

            log.warn("[NET-Heal] {}", message);

            notificationService.notifyAdministrator(
                    "Network Healing – Missing Metric",
                    message
            );

            return HealingResult.builder()
                    .action("Network Monitoring")
                    .beforeValue("UNKNOWN")
                    .afterValue("UNKNOWN")
                    .success(false)
                    .status(HealingStatus.UNRESOLVED)
                    .message(message)
                    .errorMessage("Network usage metric is null")
                    .executionTime(System.currentTimeMillis() - startTime)
                    .build();
        }

        String beforeValue =
                String.format("%.1f%%", networkUsageValue);

        log.warn(
                "[NET-Heal] Network utilization detected: {} on device '{}' ({})",
                beforeValue,
                device.getName(),
                device.getIpAddress()
        );

        /*
         * -------------------------------------------------------------
         * STEP 1: Validate network threshold
         * -------------------------------------------------------------
         */

        if (networkUsageValue < networkThreshold) {

            String message = String.format(
                    "Network utilization is currently %.1f%%, which is "
                            + "below the configured threshold of %.1f%%. "
                            + "No remediation is required.",
                    networkUsageValue,
                    networkThreshold
            );

            log.info("[NET-Heal] {}", message);

            return HealingResult.builder()
                    .action("Network Condition Verification")
                    .beforeValue(beforeValue)
                    .afterValue(beforeValue)
                    .success(true)
                    .status(HealingStatus.SUCCESS)
                    .message(message)
                    .executionTime(System.currentTimeMillis() - startTime)
                    .build();
        }

        /*
         * -------------------------------------------------------------
         * STEP 2: Collect interface statistics
         * -------------------------------------------------------------
         */

        String interfaceStats = collectInterfaceStats();

        /*
         * -------------------------------------------------------------
         * STEP 3: Identify top network process
         * -------------------------------------------------------------
         */

        String topProcessInfo = identifyTopNetworkProcess();

        String processName = extractProcessName(topProcessInfo);
        String processId = extractProcessId(topProcessInfo);

        log.info(
                "[NET-Heal] Top network process: {}",
                topProcessInfo
        );

        /*
         * -------------------------------------------------------------
         * STEP 4: Determine whether automatic remediation is safe
         * -------------------------------------------------------------
         */

        boolean protectedProcess =
                isProtectedProcess(processName);

        if (!processRemediationEnabled) {

            String message = String.format(
                    "High network utilization detected: %.1f%% on device "
                            + "'%s' (%s).%n"
                            + "Interface statistics:%n%s%n"
                            + "Top network process: %s%n"
                            + "Automatic process remediation is disabled by "
                            + "policy. Administrator review is required.",
                    networkUsageValue,
                    device.getName(),
                    device.getIpAddress(),
                    interfaceStats,
                    topProcessInfo
            );

            log.warn("[NET-Heal] {}", message);

            notificationService.notifyAdministrator(
                    "Network Utilization Requires Review",
                    message
            );

            return HealingResult.builder()
                    .action("Network Diagnostic and Administrator Notification")
                    .beforeValue(beforeValue)
                    .afterValue(beforeValue)
                    .success(false)
                    .status(HealingStatus.ADMIN_REVIEW_REQUIRED)
                    .message(message)
                    .processId(processId)
                    .processName(processName)
                    .executionTime(System.currentTimeMillis() - startTime)
                    .build();
        }

        /*
         * -------------------------------------------------------------
         * STEP 5: Never automatically terminate protected processes
         * -------------------------------------------------------------
         */

        if (protectedProcess) {

            String message = String.format(
                    "High network utilization detected: %.1f%% on device "
                            + "'%s'.%n"
                            + "Top network process: %s%n"
                            + "The identified process '%s' is protected and "
                            + "must not be terminated automatically.%n"
                            + "Administrator review is required.",
                    networkUsageValue,
                    device.getName(),
                    topProcessInfo,
                    processName
            );

            log.warn("[NET-Heal] Protected process detected: {}", processName);

            notificationService.notifyAdministrator(
                    "Protected Network Process Requires Review",
                    message
            );

            return HealingResult.builder()
                    .action("Protected Process Detection and Administrator Notification")
                    .beforeValue(beforeValue)
                    .afterValue(beforeValue)
                    .success(false)
                    .status(HealingStatus.ADMIN_REVIEW_REQUIRED)
                    .message(message)
                    .processId(processId)
                    .processName(processName)
                    .executionTime(System.currentTimeMillis() - startTime)
                    .build();
        }

        /*
         * -------------------------------------------------------------
         * STEP 6: Process identified but PID unavailable
         * -------------------------------------------------------------
         */

        if (processId == null || processId.isBlank()) {

            String message = String.format(
                    "High network utilization of %.1f%% was detected, "
                            + "but a safe process ID could not be identified.%n"
                            + "Top network process information: %s%n"
                            + "Administrator review is required.",
                    networkUsageValue,
                    topProcessInfo
            );

            log.warn("[NET-Heal] {}", message);

            notificationService.notifyAdministrator(
                    "Network Healing – Process Identification Required",
                    message
            );

            return HealingResult.builder()
                    .action("Network Process Identification")
                    .beforeValue(beforeValue)
                    .afterValue(beforeValue)
                    .success(false)
                    .status(HealingStatus.ADMIN_REVIEW_REQUIRED)
                    .message(message)
                    .processName(processName)
                    .executionTime(System.currentTimeMillis() - startTime)
                    .build();
        }

        /*
         * -------------------------------------------------------------
         * STEP 7: Attempt safe remediation
         * -------------------------------------------------------------
         */

        String remediationResult =
                terminateNonCriticalProcess(processId, processName);

        if (!remediationResult.startsWith("SUCCESS")) {

            String message = String.format(
                    "High network utilization of %.1f%% detected.%n"
                            + "Process: %s%n"
                            + "PID: %s%n"
                            + "Automatic remediation was not performed.%n"
                            + "Reason: %s",
                    networkUsageValue,
                    processName,
                    processId,
                    remediationResult
            );

            log.warn("[NET-Heal] {}", message);

            notificationService.notifyAdministrator(
                    "Network Healing Failed",
                    message
            );

            return HealingResult.builder()
                    .action("Network Process Remediation")
                    .beforeValue(beforeValue)
                    .afterValue(beforeValue)
                    .success(false)
                    .status(HealingStatus.FAILED)
                    .message(message)
                    .processId(processId)
                    .processName(processName)
                    .errorMessage(remediationResult)
                    .executionTime(System.currentTimeMillis() - startTime)
                    .build();
        }

        /*
         * -------------------------------------------------------------
         * STEP 8: Re-check process
         * -------------------------------------------------------------
         */

        boolean processStillRunning =
                isProcessRunning(processId);

        if (processStillRunning) {

            String message = String.format(
                    "Network remediation was attempted for process '%s' "
                            + "(PID %s), but the process is still running. "
                            + "Network condition could not be verified as "
                            + "resolved.",
                    processName,
                    processId
            );

            log.error("[NET-Heal] {}", message);

            notificationService.notifyAdministrator(
                    "Network Healing Verification Failed",
                    message
            );

            return HealingResult.builder()
                    .action("Network Process Remediation")
                    .beforeValue(beforeValue)
                    .afterValue(beforeValue)
                    .success(false)
                    .status(HealingStatus.FAILED)
                    .message(message)
                    .processId(processId)
                    .processName(processName)
                    .errorMessage("Process remained active after remediation")
                    .executionTime(System.currentTimeMillis() - startTime)
                    .build();
        }

        /*
         * -------------------------------------------------------------
         * STEP 9: Successful remediation
         * -------------------------------------------------------------
         *
         * The process was actually stopped and verified.
         *
         * NOTE:
         * We do not claim that network utilization has fallen until
         * a fresh metric sample confirms it.
         */

        String afterValue = "PROCESS_STOPPED";

        String message = String.format(
                "Network remediation completed successfully on device "
                        + "'%s'.%n"
                        + "Network utilization before remediation: %.1f%%%n"
                        + "Non-critical process stopped: %s%n"
                        + "PID: %s%n"
                        + "The process was verified as no longer running. "
                        + "A new network metric sample should be collected "
                        + "to confirm utilization has returned to normal.",
                device.getName(),
                networkUsageValue,
                processName,
                processId
        );

        log.info("[NET-Heal] {}", message);

        return HealingResult.builder()
                .action("Safe Network Process Remediation")
                .beforeValue(beforeValue)
                .afterValue(afterValue)
                .success(true)
                .status(HealingStatus.SUCCESS)
                .message(message)
                .processId(processId)
                .processName(processName)
                .executionTime(System.currentTimeMillis() - startTime)
                .build();
    }

    /**
     * Collect network interface statistics.
     */
    private String collectInterfaceStats() {

        try {

            Process process = new ProcessBuilder(
                    "bash",
                    "-c",
                    "netstat -ib 2>/dev/null | head -20"
            )
                    .redirectErrorStream(true)
                    .start();

            StringBuilder output = new StringBuilder();

            try (BufferedReader reader =
                         new BufferedReader(
                                 new InputStreamReader(
                                         process.getInputStream()))) {

                String line;

                while ((line = reader.readLine()) != null) {

                    if (output.length() > 10000) {
                        break;
                    }

                    output.append("  ")
                            .append(line)
                            .append("\n");
                }
            }

            process.waitFor(
                    commandTimeoutSeconds,
                    TimeUnit.SECONDS
            );

            if (output.length() == 0) {
                return "(no interface statistics available)";
            }

            return output.toString();

        } catch (Exception e) {

            log.warn(
                    "[NET-Heal] Unable to collect interface statistics: {}",
                    e.getMessage()
            );

            return "(error collecting interface statistics: "
                    + e.getMessage()
                    + ")";
        }
    }

    /**
     * Identify processes that currently have network connections.
     *
     * This is a best-effort diagnostic operation.
     */
    private String identifyTopNetworkProcess() {

        try {

            Process process = new ProcessBuilder(
                    "bash",
                    "-c",
                    "lsof -n -P -i 2>/dev/null "
                            + "| awk 'NR>1 {print $1, $2}' "
                            + "| sort | uniq -c | sort -rn | head -5"
            )
                    .redirectErrorStream(true)
                    .start();

            StringBuilder output = new StringBuilder();

            try (BufferedReader reader =
                         new BufferedReader(
                                 new InputStreamReader(
                                         process.getInputStream()))) {

                String line;

                int count = 0;

                while ((line = reader.readLine()) != null
                        && count < 5) {

                    output.append(line.trim())
                            .append("; ");

                    count++;
                }
            }

            process.waitFor(
                    commandTimeoutSeconds,
                    TimeUnit.SECONDS
            );

            if (output.length() == 0) {
                return "Could not identify network process";
            }

            return output.toString();

        } catch (Exception e) {

            log.warn(
                    "[NET-Heal] Unable to identify network process: {}",
                    e.getMessage()
            );

            return "Error identifying network process: "
                    + e.getMessage();
        }
    }

    /**
     * Extract process name from lsof output.
     *
     * Expected format:
     *
     * 36 mongod 123
     *
     * or:
     *
     * 36 mongod 123; 27 java 456;
     */
    private String extractProcessName(String processInfo) {

        if (processInfo == null
                || processInfo.isBlank()
                || processInfo.startsWith("Could not")
                || processInfo.startsWith("Error")) {

            return "UNKNOWN";
        }

        try {

            String firstEntry =
                    processInfo.split(";")[0].trim();

            String[] parts =
                    firstEntry.split("\\s+");

            if (parts.length >= 2) {
                return parts[1];
            }

        } catch (Exception e) {

            log.debug(
                    "[NET-Heal] Could not parse process name: {}",
                    e.getMessage()
            );
        }

        return "UNKNOWN";
    }

    /**
     * Extract process ID from diagnostic output.
     */
    private String extractProcessId(String processInfo) {

        if (processInfo == null
                || processInfo.isBlank()) {

            return null;
        }

        try {

            String firstEntry =
                    processInfo.split(";")[0].trim();

            String[] parts =
                    firstEntry.split("\\s+");

            /*
             * Expected:
             *
             * count processName pid
             */
            if (parts.length >= 3
                    && parts[2].matches("\\d+")) {

                return parts[2];
            }

        } catch (Exception e) {

            log.debug(
                    "[NET-Heal] Could not parse process ID: {}",
                    e.getMessage()
            );
        }

        return null;
    }

    /**
     * Check whether the process is protected.
     */
    private boolean isProtectedProcess(String processName) {

        if (processName == null) {
            return true;
        }

        String normalized =
                processName.trim().toLowerCase();

        return PROTECTED_PROCESSES.stream()
                .map(String::toLowerCase)
                .anyMatch(normalized::equals);
    }

    /**
     * Attempt to terminate a non-critical process.
     *
     * The process is first checked against the protected-process list.
     */
    private String terminateNonCriticalProcess(
            String processId,
            String processName) {

        if (!processRemediationEnabled) {

            return "Automatic process remediation is disabled";
        }

        if (processId == null
                || !processId.matches("\\d+")) {

            return "Invalid process ID";
        }

        if (isProtectedProcess(processName)) {

            return "Protected process cannot be terminated automatically";
        }

        try {

            /*
             * Graceful termination first.
             *
             * We intentionally do not use kill -9.
             */
            Process process = new ProcessBuilder(
                    "kill",
                    "-TERM",
                    processId
            )
                    .redirectErrorStream(true)
                    .start();

            boolean completed =
                    process.waitFor(
                            commandTimeoutSeconds,
                            TimeUnit.SECONDS
                    );

            if (!completed) {

                process.destroy();

                return "Process termination command timed out";
            }

            if (process.exitValue() != 0) {

                return "Process termination command failed";
            }

            return "SUCCESS: non-critical process termination requested";

        } catch (Exception e) {

            return "Process termination error: "
                    + e.getMessage();
        }
    }

    /**
     * Verify whether a process is still running.
     */
    private boolean isProcessRunning(String processId) {

        if (processId == null
                || !processId.matches("\\d+")) {

            return false;
        }

        try {

            Process process = new ProcessBuilder(
                    "kill",
                    "-0",
                    processId
            )
                    .redirectErrorStream(true)
                    .start();

            boolean completed =
                    process.waitFor(
                            commandTimeoutSeconds,
                            TimeUnit.SECONDS
                    );

            if (!completed) {
                process.destroy();
                return true;
            }

            return process.exitValue() == 0;

        } catch (Exception e) {

            log.debug(
                    "[NET-Heal] Process verification error: {}",
                    e.getMessage()
            );

            return false;
        }
    }
}