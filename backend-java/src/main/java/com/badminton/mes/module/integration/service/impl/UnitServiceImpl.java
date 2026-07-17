package com.badminton.mes.module.integration.service.impl;

import com.badminton.mes.common.enums.CommonStatusEnum;
import com.badminton.mes.module.integration.dal.repository.UnitRepository;
import com.badminton.mes.module.integration.service.UnitService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 计量单位服务实现。
 *
 * <p>为生产模块提供“锁定并确认单位可用”的窄接口。悲观写锁会保持到调用方事务结束，
 * 防止产品或工单完成单位校验后，该单位被并发停用或逻辑删除。
 *
 * @author 张竹灏
 * @date 2026/07/12
 */
@Service
public class UnitServiceImpl implements UnitService {

    /** 计量单位仓储，提供带逻辑删除过滤的悲观写锁查询。 */
    private final UnitRepository unitRepository;

    /**
     * 构造器注入。
     *
     * @param unitRepository 计量单位 Repository
     */
    public UnitServiceImpl(UnitRepository unitRepository) {
        this.unitRepository = unitRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean lockAndCheckEnabled(Long unitId) {
        // 锁定命中行后再判断启用状态，使“存在性 + 状态”校验对应同一稳定数据库版本。
        return unitRepository.findByIdAndDeletedFalseForUpdate(unitId)
                .filter(unit -> CommonStatusEnum.ENABLED.getStatus().equals(unit.getStatus()))
                .isPresent();
    }
}
