package com.badminton.mes.module.andon.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 安灯异常超时扫描任务入口。
 *
 * <p>调度器只负责按配置的固定延迟触发扫描，不在调度线程中重复实现响应超时、自动升级或通知规则。
 * 具体事件由服务层逐条放入 {@code REQUIRES_NEW} 事务处理，使某一事件的锁冲突、数据异常或通知记录失败
 * 不会回滚同一轮中已经成功处理的其他事件。固定延迟从上一次执行结束后计时，可避免扫描耗时较长时并发叠加任务。
 * 调度入口不捕获批次级异常，候选事件级错误隔离和失败审计统一由事件服务负责。
 */
@Component
public class AndonTimeoutScheduler {

    /** 统一承载候选事件查询、超时状态迁移、日志与通知副作用的事件服务。 */
    private final AndonEventService eventService;

    /**
     * 注入事件服务；调度层保持无状态，便于集群调度策略与业务处理逻辑独立演进。
     *
     * @param eventService 安灯事件业务服务
     */
    public AndonTimeoutScheduler(AndonEventService eventService) {
        this.eventService = eventService;
    }

    /**
     * 触发一轮安灯超时扫描。
     *
     * <p>初始延迟和扫描间隔均可由配置覆盖，默认一分钟。返回的处理数量由调度器刻意忽略，
     * 因为任务成功与否以服务层对每个候选事件的独立处理结果为准。单条失败由服务独立审计；若失败审计
     * 本身也未能落库，则保留原截止时间供后续轮次重试。固定延迟语义保证本轮服务调用返回后才开始
     * 计算下一轮间隔。
     */
    @Scheduled(
            initialDelayString = "${mes.andon.timeout-scan-initial-delay-ms:60000}",
            fixedDelayString = "${mes.andon.timeout-scan-interval-ms:60000}")
    public void processTimeoutEvents() {
        eventService.processTimeoutEvents();
    }
}
