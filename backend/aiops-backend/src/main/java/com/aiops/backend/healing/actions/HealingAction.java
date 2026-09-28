package com.aiops.backend.healing.actions;

import com.aiops.backend.entity.Device;
import com.aiops.backend.entity.Metric;

/**
 * Strategy interface implemented by every per-metric healing action.
 */
public interface HealingAction {

    /**
     * Execute the healing workflow appropriate for the given metric condition.
     *
     * @param device the device on which the anomaly was detected
     * @param metric the metric snapshot that triggered the anomaly
     * @return a structured HealingResult – never null
     */
    HealingResult execute(Device device, Metric metric);
}