package com.aiops.backend.service.impl;

import com.aiops.backend.entity.Metric;
import com.aiops.backend.service.RootCauseService;
import org.springframework.stereotype.Service;

@Service
public class RootCauseServiceImpl implements RootCauseService {

    @Override
    public String determineRootCause(Metric metric) {

        if (metric.getCpuUsage() >= 90) {
            return "CPU overload detected due to sustained high CPU utilization.";
        }

        if (metric.getMemoryUsage() >= 90) {
            return "Memory exhaustion detected due to critically high memory utilization.";
        }

        if (metric.getDiskUsage() >= 90) {
            return "Disk resource exhaustion detected due to critically high disk utilization.";
        }

        if (metric.getPacketLoss() >= 5
                && metric.getLatency() >= 200) {
            return "Network degradation detected due to high packet loss and latency.";
        }

        if (metric.getPacketLoss() >= 5) {
            return "Network instability detected due to high packet loss.";
        }

        if (metric.getLatency() >= 200) {
            return "Network congestion detected due to high latency.";
        }

        return "Abnormal system behavior detected by the machine learning model.";
    }

    @Override
    public String determineRecommendedAction(Metric metric) {

        if (metric.getCpuUsage() >= 90) {
            return "Reduce CPU workload or restart the affected monitoring service.";
        }

        if (metric.getMemoryUsage() >= 90) {
            return "Release unused memory or restart the affected service.";
        }

        if (metric.getDiskUsage() >= 90) {
            return "Free disk space or remove unnecessary files.";
        }

        if (metric.getPacketLoss() >= 5
                && metric.getLatency() >= 200) {
            return "Check network connectivity, routing and restart the network service if required.";
        }

        if (metric.getPacketLoss() >= 5) {
            return "Check network connectivity and packet transmission.";
        }

        if (metric.getLatency() >= 200) {
            return "Check network congestion and connectivity.";
        }

        return "Investigate the affected device and review recent metric trends.";
    }
}