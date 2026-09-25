package com.aiops.backend.service;

public interface HealingService {

    boolean heal(Long deviceId, String rootCause);
}