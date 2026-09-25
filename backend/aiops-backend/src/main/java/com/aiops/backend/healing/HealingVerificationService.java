package com.aiops.backend.healing;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HealingVerificationService {

    private final RecoveryExecutor recoveryExecutor;

    public boolean verifyRecovery(Long deviceId) {

        boolean recovered =
                recoveryExecutor.isDeviceRecovered(deviceId);

        System.out.println("========== RECOVERY VERIFICATION ==========");
        System.out.println("Device ID: " + deviceId);
        System.out.println("Verification Result: "
                + (recovered ? "RECOVERED" : "NOT RECOVERED"));
        System.out.println("===========================================");

        return recovered;
    }
}