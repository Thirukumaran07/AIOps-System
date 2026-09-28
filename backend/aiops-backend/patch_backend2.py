import os

base_dir = "/Users/apple/Project/AIops-network-self-healing/backend/aiops-backend/src/main/java/com/aiops/backend"

# HealingLog.java
healing_log_code = """package com.aiops.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "healing_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealingLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    @Column(name = "metric_id")
    private Long metricId;

    @Column(name = "root_cause", nullable = false)
    private String rootCause;

    @Column(name = "action_taken", nullable = false)
    private String actionTaken;

    @Column(nullable = false)
    private String status;
    
    @Column(nullable = false)
    private Boolean success;

    @Column(name = "before_value")
    private String beforeValue;

    @Column(name = "after_value")
    private String afterValue;

    @Column(name = "process_id")
    private String processId;

    @Column(name = "process_name")
    private String processName;

    @Column(length = 500)
    private String message;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
"""

# HealingServiceImpl.java
healing_service_impl_code = """package com.aiops.backend.service.impl;

import com.aiops.backend.entity.HealingLog;
import com.aiops.backend.entity.Metric;
import com.aiops.backend.entity.Device;
import com.aiops.backend.repository.HealingLogRepository;
import com.aiops.backend.repository.MetricRepository;
import com.aiops.backend.repository.DeviceRepository;
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
    private final MetricRepository metricRepository;
    private final DeviceRepository deviceRepository;
    private final AlertService alertService;
    
    private final CpuHealingAction cpuHealingAction;
    private final MemoryHealingAction memoryHealingAction;
    private final DiskHealingAction diskHealingAction;
    private final NetworkHealingAction networkHealingAction;

    @Override
    public boolean heal(Long deviceId, String rootCause) {
        Device device = deviceRepository.findById(deviceId).orElse(null);
        if (device == null) return false;
        
        Metric metric = metricRepository.findByDeviceIdOrderByTimestampDesc(deviceId)
            .stream().findFirst().orElse(null);
            
        if (metric == null) return false;

        HealingAction actionToExecute = null;

        if (rootCause.contains("CPU") || rootCause.contains("Cpu")) {
            actionToExecute = cpuHealingAction;
        } else if (rootCause.contains("memory") || rootCause.contains("Memory")) {
            actionToExecute = memoryHealingAction;
        } else if (rootCause.contains("disk") || rootCause.contains("Disk")) {
            actionToExecute = diskHealingAction;
        } else if (rootCause.contains("network") || rootCause.contains("latency") || rootCause.contains("packet") || rootCause.contains("Network")) {
            actionToExecute = networkHealingAction;
        }

        if (actionToExecute == null) {
            log.warn("No automated healing available for: " + rootCause);
            return false;
        }

        try {
            HealingResult result = actionToExecute.execute(device, metric);
            
            HealingLog healingLog = HealingLog.builder()
                    .deviceId(deviceId)
                    .metricId(metric.getId())
                    .rootCause(rootCause)
                    .actionTaken(result.getAction())
                    .status(result.isSuccess() ? "SUCCESS" : "FAILED")
                    .success(result.isSuccess())
                    .message(result.getMessage())
                    .errorMessage(result.getErrorMessage())
                    .processName(result.getProcessName())
                    .timestamp(LocalDateTime.now())
                    .build();
            healingLogRepository.save(healingLog);

            if (!result.isSuccess()) {
                alertService.createAlert(
                        deviceId,
                        "HEALING_FAILED",
                        "HIGH",
                        "Healing failed: " + result.getMessage(),
                        metric.getAnomalyScore(),
                        rootCause,
                        "Manual administrator intervention required"
                );
            }

            return result.isSuccess();
        } catch (Exception e) {
            log.error("Healing execution failed", e);
            HealingLog errorLog = HealingLog.builder()
                    .deviceId(deviceId)
                    .metricId(metric.getId())
                    .rootCause(rootCause)
                    .actionTaken("Attempted automated healing")
                    .status("FAILED")
                    .success(false)
                    .errorMessage(e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
            healingLogRepository.save(errorLog);
            
            alertService.createAlert(
                    deviceId,
                    "HEALING_ERROR",
                    "CRITICAL",
                    "Exception during healing: " + e.getMessage(),
                    metric.getAnomalyScore(),
                    rootCause,
                    "Check backend logs"
            );
            return false;
        }
    }
}
"""

# RecoveryExecutor.java
recovery_executor_code = """package com.aiops.backend.healing;

import org.springframework.stereotype.Component;

@Component
public class RecoveryExecutor {

    public boolean execute(Long deviceId, String healingAction) {
        return true;
    }
    
    public boolean isDeviceRecovered(Long deviceId) {
        return true;
    }
}
"""

with open(f"{base_dir}/entity/HealingLog.java", "w") as f:
    f.write(healing_log_code)
    
with open(f"{base_dir}/service/impl/HealingServiceImpl.java", "w") as f:
    f.write(healing_service_impl_code)
    
with open(f"{base_dir}/healing/RecoveryExecutor.java", "w") as f:
    f.write(recovery_executor_code)

print("Java files patched successfully.")
