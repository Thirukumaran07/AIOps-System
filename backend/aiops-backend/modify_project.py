import os

base_dir = "/Users/apple/Project/AIops-network-self-healing/backend/aiops-backend/src/main/java/com/aiops/backend"

# 1. HealingAction.java
healing_action_code = """package com.aiops.backend.healing.actions;

import com.aiops.backend.entity.Metric;
import com.aiops.backend.entity.Device;

public interface HealingAction {
    HealingResult execute(Device device, Metric metric);
}
"""
os.makedirs(f"{base_dir}/healing/actions", exist_ok=True)
with open(f"{base_dir}/healing/actions/HealingAction.java", "w") as f:
    f.write(healing_action_code)

# 2. HealingResult.java
healing_result_code = """package com.aiops.backend.healing.actions;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HealingResult {
    private boolean success;
    private String action;
    private String message;
    private String beforeValue;
    private String afterValue;
    private String processId;
    private String processName;
    private Long executionTime;
    private String errorMessage;
}
"""
with open(f"{base_dir}/healing/actions/HealingResult.java", "w") as f:
    f.write(healing_result_code)

print("Files created")
