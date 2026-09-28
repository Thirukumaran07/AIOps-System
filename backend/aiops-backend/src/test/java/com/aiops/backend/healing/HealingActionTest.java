package com.aiops.backend.healing;

import com.aiops.backend.entity.Device;
import com.aiops.backend.entity.HealingLog;
import com.aiops.backend.entity.Metric;
import com.aiops.backend.healing.actions.*;
import com.aiops.backend.repository.DeviceRepository;
import com.aiops.backend.repository.HealingLogRepository;
import com.aiops.backend.repository.MetricRepository;
import com.aiops.backend.service.AlertService;
import com.aiops.backend.service.impl.HealingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the self-healing pipeline.
 *
 * Uses plain Mockito.mock() (no ByteBuddy inline mock maker) so the tests
 * work reliably on JDK 21–26.
 *
 * The six concrete HealingAction classes all implement HealingAction,
 * so we cast their mocks at injection-time – HealingServiceImpl accepts them
 * by their concrete types through constructor injection.
 */
@DisplayName("Self-Healing Pipeline Tests")
class HealingActionTest {

    // ── manually-created mocks (no @ExtendWith needed) ────────────────────────
    private final HealingLogRepository healingLogRepo  = mock(HealingLogRepository.class);
    private final MetricRepository     metricRepo      = mock(MetricRepository.class);
    private final DeviceRepository     deviceRepo      = mock(DeviceRepository.class);
    private final AlertService         alertService    = mock(AlertService.class);
    private final NotificationService  notifService    = mock(NotificationService.class);

    // Actions are mocked via anonymous subclasses to bypass JDK 26 Mockito issues
    // with concrete classes and inline mock makers.
    private final CpuHealingAction cpuAction = new CpuHealingAction(notifService) {
        @Override public HealingResult execute(Device d, Metric m) { return mockCpuAction(d, m); }
    };
    private final MemoryHealingAction memAction = new MemoryHealingAction(notifService) {
        @Override public HealingResult execute(Device d, Metric m) { return mockMemAction(d, m); }
    };
    private final DiskHealingAction diskAction = new DiskHealingAction(notifService) {
        @Override public HealingResult execute(Device d, Metric m) { return mockDiskAction(d, m); }
    };
    private final NetworkHealingAction netAction = new NetworkHealingAction(notifService) {
        @Override public HealingResult execute(Device d, Metric m) { return mockNetAction(d, m); }
    };
    private final LatencyHealingAction latAction = new LatencyHealingAction(notifService) {
        @Override public HealingResult execute(Device d, Metric m) { return mockLatAction(d, m); }
    };
    private final PacketLossHealingAction pktAction = new PacketLossHealingAction(notifService) {
        @Override public HealingResult execute(Device d, Metric m) { return mockPktAction(d, m); }
    };

    // Programmable mock responses
    private HealingResult cpuResult, memResult, diskResult, netResult, latResult, pktResult;
    private boolean cpuCalled, memCalled, diskCalled, netCalled, latCalled, pktCalled;

    private HealingResult mockCpuAction(Device d, Metric m) { cpuCalled = true; return cpuResult; }
    private HealingResult mockMemAction(Device d, Metric m) { memCalled = true; return memResult; }
    private HealingResult mockDiskAction(Device d, Metric m) { diskCalled = true; return diskResult; }
    private HealingResult mockNetAction(Device d, Metric m) { netCalled = true; return netResult; }
    private HealingResult mockLatAction(Device d, Metric m) { latCalled = true; return latResult; }
    private HealingResult mockPktAction(Device d, Metric m) { pktCalled = true; return pktResult; }

    private HealingServiceImpl healingService;
    private Device device;
    private Metric metric;

    @BeforeEach
    void setUp() {
        // Reset all mocks before each test
        reset(healingLogRepo, metricRepo, deviceRepo, alertService, notifService);
        cpuCalled = memCalled = diskCalled = netCalled = latCalled = pktCalled = false;

        healingService = new HealingServiceImpl(
                healingLogRepo, metricRepo, deviceRepo,
                alertService, notifService,
                cpuAction, memAction, diskAction, netAction, latAction, pktAction);

        device = Device.builder()
                .id(1L).name("test-device").ipAddress("127.0.0.1")
                .type("SERVER").status("ONLINE").healthScore(100.0)
                .build();

        metric = Metric.builder()
                .id(10L).device(device)
                .cpuUsage(20.0).memoryUsage(20.0).diskUsage(20.0)
                .networkUsage(20.0).latency(50.0).packetLoss(0.0)
                .anomalyStatus("NORMAL").anomalyScore(0.1)
                .timestamp(LocalDateTime.now())
                .build();

        when(deviceRepo.findById(1L)).thenReturn(Optional.of(device));
        when(metricRepo.findByDeviceIdOrderByTimestampDesc(1L)).thenReturn(List.of(metric));
        when(healingLogRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 1. CPU tests
    // ══════════════════════════════════════════════════════════════════════════

    @Test @DisplayName("1. CPU >= 90, non-critical process → RECOVERED")
    void cpu_highUsage_nonCritical_recovered() {
        metric.setCpuUsage(94.0);
        cpuResult = recoveredResult("CPU Remediation",
                "94.0% CPU", "55.0% CPU", "9999", "python-test", "Terminated python-test");

        assertThat(healingService.heal(1L, "High CPU utilization")).isTrue();
        verifyLogStatus("RECOVERED");
        verifyNoAlert();
    }

    @Test @DisplayName("2. CPU < 90 → no action taken, success")
    void cpu_normalUsage_noAction() {
        metric.setCpuUsage(50.0);
        cpuResult = HealingResult.builder()
                .action("CPU Remediation").success(true)
                .status(HealingStatus.RECOVERED)
                .message("CPU 50.0% is below threshold.").build();

        assertThat(healingService.heal(1L, "High CPU utilization")).isTrue();
        assertThat(cpuCalled).isTrue();
    }

    @Test @DisplayName("3. CPU >= 90, CRITICAL process → ADMIN_NOTIFICATION_REQUIRED")
    void cpu_highUsage_criticalProcess_adminNotification() {
        metric.setCpuUsage(95.0);
        cpuResult = HealingResult.builder()
                .action("CPU Remediation").success(false)
                .status(HealingStatus.ADMIN_NOTIFICATION_REQUIRED)
                .processId("1").processName("java")
                .message("java is critical – NOT terminated.").build();

        assertThat(healingService.heal(1L, "High CPU utilization")).isFalse();
        verifyLogStatus("ADMIN_NOTIFICATION_REQUIRED");
        verifyAlertCreated();
    }

    @Test @DisplayName("4. CPU – non-critical process terminated successfully")
    void cpu_nonCriticalProcess_terminated() {
        metric.setCpuUsage(92.0);
        cpuResult = recoveredResult("CPU Remediation",
                "92.0% CPU", "35.0% CPU", "5555", "test-proc", "Terminated test-proc");

        assertThat(healingService.heal(1L, "High CPU utilization")).isTrue();
        verifyLogStatus("RECOVERED");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. Memory tests
    // ══════════════════════════════════════════════════════════════════════════

    @Test @DisplayName("5. Memory >= 90 → RECOVERED")
    void memory_highUsage_recovered() {
        metric.setMemoryUsage(92.0);
        memResult = HealingResult.builder()
                .action("Memory Remediation").success(true)
                .status(HealingStatus.RECOVERED).message("Memory released.").build();

        assertThat(healingService.heal(1L, "High memory utilization")).isTrue();
        assertThat(memCalled).isTrue();
        verifyLogStatus("RECOVERED");
    }

    @Test @DisplayName("6. Memory – critical process → ADMIN_NOTIFICATION_REQUIRED")
    void memory_criticalProcess_adminNotification() {
        metric.setMemoryUsage(91.0);
        memResult = HealingResult.builder()
                .action("Memory Remediation").success(false)
                .status(HealingStatus.ADMIN_NOTIFICATION_REQUIRED)
                .message("java is critical, not terminated.").build();

        healingService.heal(1L, "High memory utilization");
        verifyAlertCreated();
        verifyLogStatus("ADMIN_NOTIFICATION_REQUIRED");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. Disk tests
    // ══════════════════════════════════════════════════════════════════════════

    @Test @DisplayName("7. Disk >= 90, safe files available → RECOVERED")
    void disk_highUsage_safeCleanup() {
        metric.setDiskUsage(92.0);
        diskResult = HealingResult.builder()
                .action("Disk Remediation").success(true)
                .status(HealingStatus.RECOVERED)
                .beforeValue("92% used").afterValue("78% used")
                .message("Deleted 5 safe temp files.").build();

        assertThat(healingService.heal(1L, "High disk utilization")).isTrue();
        assertThat(diskCalled).isTrue();
        verifyLogStatus("RECOVERED");
    }

    @Test @DisplayName("8. Disk >= 90, no safe cleanup → ADMIN_REVIEW_REQUIRED")
    void disk_highUsage_noSafeCleanup() {
        metric.setDiskUsage(93.0);
        diskResult = HealingResult.builder()
                .action("Disk Remediation").success(false)
                .status(HealingStatus.ADMIN_REVIEW_REQUIRED)
                .message("No safe files to clean. Admin review required.").build();

        healingService.heal(1L, "High disk utilization");
        verifyAlertCreated();
        verifyLogStatus("ADMIN_REVIEW_REQUIRED");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 4. Network test
    // ══════════════════════════════════════════════════════════════════════════

    @Test @DisplayName("9. High network utilization → ADMIN_NOTIFICATION_REQUIRED")
    void network_highUtilization_adminNotification() {
        metric.setNetworkUsage(95.0);
        netResult = HealingResult.builder()
                .action("Network Diagnostic Remediation").success(true)
                .status(HealingStatus.ADMIN_NOTIFICATION_REQUIRED)
                .message("High network utilization. Admin notified.").build();

        healingService.heal(1L, "High network utilization");
        assertThat(netCalled).isTrue();
        verifyAlertCreated();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 5. Latency tests
    // ══════════════════════════════════════════════════════════════════════════

    @Test @DisplayName("10. Latency >= 200 ms, recovers after retry → RECOVERED")
    void latency_recoversAfterRetry() {
        metric.setLatency(350.0);
        latResult = HealingResult.builder()
                .action("Latency Remediation").success(true)
                .status(HealingStatus.RECOVERED)
                .beforeValue("350.0 ms").afterValue("120.0 ms").retryCount(2)
                .message("RECOVERED on retry 2. Before: 350.0 ms, After: 120.0 ms.").build();

        assertThat(healingService.heal(1L, "High network latency")).isTrue();
        assertThat(latCalled).isTrue();
        verifyLogStatus("RECOVERED");
    }

    @Test @DisplayName("11. Latency persists after all retries → UNRESOLVED + alert")
    void latency_persistsAfterRetries() {
        metric.setLatency(350.0);
        latResult = HealingResult.builder()
                .action("Latency Remediation").success(false)
                .status(HealingStatus.UNRESOLVED)
                .beforeValue("350.0 ms").afterValue("310.0 ms").retryCount(3)
                .message("UNRESOLVED after 3 retries.").build();

        assertThat(healingService.heal(1L, "High network latency")).isFalse();
        verifyAlertCreated();
        verifyLogStatus("UNRESOLVED");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 6. Packet-loss tests
    // ══════════════════════════════════════════════════════════════════════════

    @Test @DisplayName("12. Packet loss >= 5%, recovers after retry → RECOVERED")
    void packetLoss_recoversAfterRetry() {
        metric.setPacketLoss(10.0);
        pktResult = HealingResult.builder()
                .action("Packet-Loss Remediation").success(true)
                .status(HealingStatus.RECOVERED)
                .beforeValue("10.0%").afterValue("0.0%").retryCount(1)
                .message("RECOVERED on retry 1.").build();

        assertThat(healingService.heal(1L, "Network packet loss")).isTrue();
        assertThat(pktCalled).isTrue();
        verifyLogStatus("RECOVERED");
    }

    @Test @DisplayName("13. Packet loss persists → UNRESOLVED + alert")
    void packetLoss_persistsAfterRetries() {
        metric.setPacketLoss(15.0);
        pktResult = HealingResult.builder()
                .action("Packet-Loss Remediation").success(false)
                .status(HealingStatus.UNRESOLVED)
                .beforeValue("15.0%").afterValue("12.0%").retryCount(3)
                .message("UNRESOLVED after 3 retries.").build();

        healingService.heal(1L, "Network packet loss");
        verifyAlertCreated();
        verifyLogStatus("UNRESOLVED");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 7. Dry-run test
    // ══════════════════════════════════════════════════════════════════════════

    @Test @DisplayName("14. Dry-run mode → DRY_RUN status, no alert")
    void dryRun_noRealTermination() {
        metric.setCpuUsage(95.0);
        cpuResult = HealingResult.builder()
                .action("CPU Remediation").success(true)
                .status(HealingStatus.DRY_RUN)
                .message("DRY RUN - action would be executed: kill -15 9999 (python-test).").build();

        assertThat(healingService.heal(1L, "High CPU utilization")).isTrue();
        verifyLogStatus("DRY_RUN");
        verifyNoAlert();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 8. HealingLog field verification
    // ══════════════════════════════════════════════════════════════════════════

    @Test @DisplayName("15. HealingLog is saved with all correct fields")
    void healingLog_allFieldsCorrect() {
        metric.setCpuUsage(94.0);
        cpuResult = recoveredResult(
                "CPU Remediation", "94.0% CPU", "40.0% CPU", "1234", "python-test", "Terminated python-test");

        healingService.heal(1L, "High CPU utilization");

        ArgumentCaptor<HealingLog> captor = ArgumentCaptor.forClass(HealingLog.class);
        verify(healingLogRepo).save(captor.capture());

        HealingLog saved = captor.getValue();
        assertThat(saved.getDeviceId()).isEqualTo(1L);
        assertThat(saved.getMetricId()).isEqualTo(10L);
        assertThat(saved.getRootCause()).isEqualTo("High CPU utilization");
        assertThat(saved.getActionTaken()).isEqualTo("CPU Remediation");
        assertThat(saved.getStatus()).isEqualTo("RECOVERED");
        assertThat(saved.getSuccess()).isTrue();
        assertThat(saved.getProcessId()).isEqualTo("1234");
        assertThat(saved.getProcessName()).isEqualTo("python-test");
        assertThat(saved.getBeforeValue()).isEqualTo("94.0% CPU");
        assertThat(saved.getAfterValue()).isEqualTo("40.0% CPU");
        assertThat(saved.getMessage()).isEqualTo("Terminated python-test");
        assertThat(saved.getTimestamp()).isNotNull();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 9. Alert creation
    // ══════════════════════════════════════════════════════════════════════════

    @Test @DisplayName("16. Alert created when healing FAILED")
    void alert_createdOnHealingFailure() {
        metric.setCpuUsage(97.0);
        cpuResult = HealingResult.builder()
                .action("CPU Remediation").success(false)
                .status(HealingStatus.FAILED)
                .message("Exception during remediation.").build();

        healingService.heal(1L, "High CPU utilization");
        verifyAlertCreated();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 10. ML anomaly routing
    // ══════════════════════════════════════════════════════════════════════════

    @Test @DisplayName("17. ML anomaly with high latency → routes to LatencyHealingAction")
    void mlAnomaly_highLatency_routesToLatencyAction() {
        metric.setLatency(300.0);
        latResult = HealingResult.builder()
                .action("Latency Remediation").success(true)
                .status(HealingStatus.RECOVERED).message("ok").build();

        healingService.heal(1L, "ML-detected system anomaly");
        assertThat(latCalled).isTrue();
    }

    @Test @DisplayName("18. Device not found → returns false without exception")
    void deviceNotFound_gracefulReturn() {
        when(deviceRepo.findById(99L)).thenReturn(Optional.empty());

        boolean result = healingService.heal(99L, "High CPU utilization");

        assertThat(result).isFalse();
        assertThat(cpuCalled).isFalse();
    }

    @Test @DisplayName("19. Administrator notification triggered when UNRESOLVED")
    void adminNotification_triggeredOnUnresolved() {
        metric.setLatency(400.0);
        latResult = HealingResult.builder()
                .action("Latency Remediation").success(false)
                .status(HealingStatus.UNRESOLVED)
                .message("UNRESOLVED after 3 retries.").build();

        healingService.heal(1L, "High network latency");
        verifyAlertCreated();
        verifyLogStatus("UNRESOLVED");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Helpers
    // ══════════════════════════════════════════════════════════════════════════

    private HealingResult recoveredResult(String action, String before, String after,
                                          String pid, String procName, String message) {
        return HealingResult.builder()
                .action(action).success(true).status(HealingStatus.RECOVERED)
                .beforeValue(before).afterValue(after)
                .processId(pid).processName(procName)
                .message(message).build();
    }

    private void verifyLogStatus(String expectedStatus) {
        ArgumentCaptor<HealingLog> captor = ArgumentCaptor.forClass(HealingLog.class);
        verify(healingLogRepo).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(expectedStatus);
    }

    private void verifyAlertCreated() {
        verify(alertService).createAlert(
                anyLong(), anyString(), anyString(), anyString(), anyDouble(), anyString(), anyString());
    }

    private void verifyNoAlert() {
        verify(alertService, never()).createAlert(
                anyLong(), anyString(), anyString(), anyString(), anyDouble(), anyString(), anyString());
    }
}
