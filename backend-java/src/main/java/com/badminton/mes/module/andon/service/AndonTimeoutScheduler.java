package com.badminton.mes.module.andon.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 安灯异常超时扫描任务。
 *
 * <p>{@link Scheduled} 由 {@code MesApplication} 上的 {@code @EnableScheduling} 开启；
 * 每次触发只负责调度 Service，不在调度线程中直接拼装业务数据。具体的超时状态更新、
 * 独立事务和失败记录由 {@link AndonEventService#processTimeoutEvents()} 完成。
 *
 * @author MES 开发组
 * @date 2026/07/16
 */
@Component
public class AndonTimeoutScheduler {

    private final AndonEventService eventService;

    public AndonTimeoutScheduler(AndonEventService eventService) {
        this.eventService = eventService;
    }

    /** 按配置间隔扫描待响应或待升级的安灯异常。 */
    @Scheduled(
            initialDelayString = "${mes.andon.timeout-scan-initial-delay-ms:60000}",
            fixedDelayString = "${mes.andon.timeout-scan-interval-ms:60000}")
    public void processTimeoutEvents() {
        eventService.processTimeoutEvents();
    }
}
