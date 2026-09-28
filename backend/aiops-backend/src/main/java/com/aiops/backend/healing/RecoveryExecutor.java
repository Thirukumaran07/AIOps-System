package com.aiops.backend.healing;

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
