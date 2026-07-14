package com.badminton.mes.module.andon.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 安灯异常超时扫描任务。 */
@Component
public class AndonTimeoutScheduler {

    private final AndonEventService eventService;

    public AndonTimeoutScheduler(AndonEventService eventService) {
        this.eventService = eventService;
    }

    @Scheduled(
            initialDelayString = "${mes.andon.timeout-scan-initial-delay-ms:60000}",
            fixedDelayString = "${mes.andon.timeout-scan-interval-ms:60000}")
    public void processTimeoutEvents() {
        eventService.processTimeoutEvents();
    }
}
