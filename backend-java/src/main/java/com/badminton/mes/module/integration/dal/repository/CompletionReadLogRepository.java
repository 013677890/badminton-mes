package com.badminton.mes.module.integration.dal.repository;

import com.badminton.mes.module.integration.dal.entity.CompletionReadLogEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * 生产完工单读取日志 Repository。
 *
 * <p>继承批量保存能力，用于一次性写入当前页每条完工单的读取痕迹；同时继承动态规格执行能力，
 * 支持按来源系统、完工单号和读取时间检索审计历史。本接口不提供删除或状态流转专用方法。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
public interface CompletionReadLogRepository extends JpaRepository<CompletionReadLogEntity, Long>,
        JpaSpecificationExecutor<CompletionReadLogEntity> {
}
