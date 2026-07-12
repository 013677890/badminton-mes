package com.badminton.mes.module.integration.service;

/**
 * 计量单位服务契约。
 *
 * @author 张竹灏
 * @date 2026/07/12
 */
public interface UnitService {

    /**
     * 对计量单位行加写锁，并判断其是否存在、未删除且启用。
     *
     * <p>调用方应在写事务中使用，使单位状态校验与后续主数据写入共享事务锁。
     *
     * @param unitId 计量单位主键
     * @return true 表示单位可用
     */
    boolean lockAndCheckEnabled(Long unitId);
}
