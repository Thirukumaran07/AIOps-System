package com.aiops.backend.healing;

import com.aiops.backend.entity.HealingLog;
import com.aiops.backend.repository.HealingLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecoveryHistory {

    private final HealingLogRepository healingLogRepository;

    public List<HealingLog> getAllHistory() {

        return healingLogRepository
                .findAll()
                .stream()
                .sorted(
                        (a, b) ->
                                b.getExecutedAt()
                                        .compareTo(a.getExecutedAt())
                )
                .toList();
    }

    public List<HealingLog> getDeviceHistory(
            Long deviceId
    ) {

        return healingLogRepository
                .findByDeviceIdOrderByExecutedAtDesc(deviceId);
    }

    public List<HealingLog> getRecentHistory() {

        return healingLogRepository
                .findTop10ByOrderByExecutedAtDesc();
    }
}