import os

base_dir = "/Users/apple/Project/AIops-network-self-healing/backend/aiops-backend/src/main/java/com/aiops/backend"

# HealingServiceImpl.java
healing_service_impl = """package com.aiops.backend.service.impl;

import com.aiops.backend.entity.HealingLog;
import com.aiops.backend.entity.Metric;
import com.aiops.backend.entity.RootCause;
import com.aiops.backend.repository.HealingLogRepository;
import com.aiops.backend.service.AlertService;
import com.aiops.backend.service.HealingService;
import com.aiops.backend.healing.actions.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealingServiceImpl implements HealingService {

    private final HealingLogRepository healingLogRepository;
    private final AlertService alertService;
    private final CpuHealingAction cpuHealingAction;
    private final MemoryHealingAction memoryHealingAction;
    private final DiskHealingAction diskHealingAction;
    private final NetworkHealingAction networkHealingAction;

    @Override
    public String initiateHealing(RootCause rootCause) {
        Metric metric = rootCause.getMetric();
        String anomaly = rootCause.getRootCause();
        HealingAction actionToExecute = null;

        if (anomaly.contains("CPU")) {
            actionToExecute = cpuHealingAction;
        } else if (anomaly.contains("memory")) {
            actionToExecute = memoryHealingAction;
        } else if (anomaly.contains("disk")) {
            actionToExecute = diskHealingAction;
        } else if (anomaly.contains("network") || anomaly.contains("latency") || anomaly.contains("packet")) {
            actionToExecute = networkHealingAction;
        }

        if (actionToExecute == null) {
            return "No automated healing available for: " + anomaly;
        }

        try {
            HealingResult result = actionToExecute.execute(metric.getDevice(), metric);
            
            HealingLog healingLog = HealingLog.builder()
                    .device(metric.getDevice())
                    .metric(metric)
                    .rootCause(rootCause.getRootCause())
                    .actionTaken(result.getAction())
                    .status(result.isSuccess() ? "SUCCESS" : "FAILED")
                    .message(result.getMessage())
                    .errorMessage(result.getErrorMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
            healingLogRepository.save(healingLog);

            if (!result.isSuccess()) {
                alertService.createAlert(metric, "Healing failed: " + result.getMessage());
            }

            return result.getMessage();
        } catch (Exception e) {
            log.error("Healing execution failed", e);
            HealingLog errorLog = HealingLog.builder()
                    .device(metric.getDevice())
                    .metric(metric)
                    .rootCause(rootCause.getRootCause())
                    .actionTaken("Attempted automated healing")
                    .status("FAILED")
                    .errorMessage(e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
            healingLogRepository.save(errorLog);
            alertService.createAlert(metric, "Exception during healing: " + e.getMessage());
            return "Healing execution failed due to an error.";
        }
    }
}
"""

# CpuHealingAction.java
cpu_healing_action = """package com.aiops.backend.healing.actions;

import com.aiops.backend.entity.Device;
import com.aiops.backend.entity.Metric;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

@Slf4j
@Component
public class CpuHealingAction implements HealingAction {

    @Value("${healing.cpu.threshold:90}")
    private double cpuThreshold;
    
    @Value("#{'${healing.cpu.safe-processes}'.split(',')}")
    private List<String> safeProcesses;

    @Override
    public HealingResult execute(Device device, Metric metric) {
        long startTime = System.currentTimeMillis();
        HealingResult.HealingResultBuilder result = HealingResult.builder().action("CPU Remediation");

        if (metric.getCpuUsage() < cpuThreshold) {
            return result.success(true).message("CPU usage below threshold, no action needed.").build();
        }

        try {
            // Find top CPU process
            Process p = new ProcessBuilder("bash", "-c", "ps -eo pid,pcpu,comm --sort=-pcpu | head -n 2 | tail -n 1").start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String topProcess = reader.readLine();
            p.waitFor();

            if (topProcess != null && !topProcess.trim().isEmpty()) {
                String[] parts = topProcess.trim().split("\\\\s+");
                if (parts.length >= 3) {
                    String pid = parts[0];
                    String pcpu = parts[1];
                    String comm = parts[2];

                    result.processId(pid).processName(comm).beforeValue(pcpu);
                    
                    if (isSafeToTerminate(comm)) {
                        Process killP = new ProcessBuilder("kill", "-15", pid).start();
                        killP.waitFor();
                        return result.success(true)
                                .message("Safely terminated non-critical process: " + comm + " (PID: " + pid + ") consuming " + pcpu + "% CPU.")
                                .executionTime(System.currentTimeMillis() - startTime)
                                .build();
                    } else {
                        return result.success(false)
                                .message("Process " + comm + " (PID: " + pid + ") is critical and cannot be terminated safely.")
                                .executionTime(System.currentTimeMillis() - startTime)
                                .build();
                    }
                }
            }
            return result.success(false).message("Could not identify high CPU process.").build();
        } catch (Exception e) {
            log.error("Failed to execute CPU remediation", e);
            return result.success(false).errorMessage(e.getMessage()).message("CPU Remediation threw an exception.").build();
        }
    }

    private boolean isSafeToTerminate(String processName) {
        return safeProcesses.stream().anyMatch(processName::contains);
    }
}
"""

with open(f"{base_dir}/service/impl/HealingServiceImpl.java", "w") as f:
    f.write(healing_service_impl)
    
with open(f"{base_dir}/healing/actions/CpuHealingAction.java", "w") as f:
    f.write(cpu_healing_action)

print("Java files written")
