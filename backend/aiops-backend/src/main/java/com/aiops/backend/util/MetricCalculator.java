package com.aiops.backend.util;

import org.springframework.stereotype.Component;

@Component
public class MetricCalculator {

    /**
     * Calculates the overall device health score.
     *
     * CPU       -> 30%
     * Memory    -> 25%
     * Disk      -> 15%
     * Latency   -> 20%
     * PacketLoss -> 10%
     */
    public double calculateHealthScore(
            double cpuUsage,
            double memoryUsage,
            double diskUsage,
            double latency,
            double packetLoss) {

        double cpuHealth = 100.0 - cpuUsage;

        double memoryHealth = 100.0 - memoryUsage;

        double diskHealth = 100.0 - diskUsage;

        double latencyHealth = calculateLatencyHealth(latency);

        double packetHealth = 100.0 - packetLoss;

        double healthScore =
                (cpuHealth * 0.30) +
                        (memoryHealth * 0.25) +
                        (diskHealth * 0.15) +
                        (latencyHealth * 0.20) +
                        (packetHealth * 0.10);

        return Math.round(healthScore * 100.0) / 100.0;
    }

    /**
     * Converts network latency into a health value between 0 and 100.
     */
    private double calculateLatencyHealth(double latency) {

        if (latency <= 50) {
            return 100.0;
        }

        if (latency <= 100) {
            return 80.0;
        }

        if (latency <= 150) {
            return 60.0;
        }

        if (latency <= 200) {
            return 40.0;
        }

        if (latency <= 300) {
            return 20.0;
        }

        return 0.0;
    }

    /**
     * Determines device status based on health score.
     */
    public String calculateStatus(double healthScore) {

        if (healthScore >= 75) {
            return "HEALTHY";
        }

        if (healthScore >= 50) {
            return "WARNING";
        }

        return "CRITICAL";
    }
}