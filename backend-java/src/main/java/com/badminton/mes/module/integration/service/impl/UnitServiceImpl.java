package com.badminton.mes.module.integration.service.impl;

import com.badminton.mes.common.enums.CommonStatusEnum;
import com.badminton.mes.module.integration.dal.repository.UnitRepository;
import com.badminton.mes.module.integration.service.UnitService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 计量单位服务实现。
 *
 * @author 张竹灏
 * @date 2026/07/12
 */
@Service
public class UnitServiceImpl implements UnitService {

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
        return unitRepository.findByIdAndDeletedFalseForUpdate(unitId)
                .filter(unit -> CommonStatusEnum.ENABLED.getStatus().equals(unit.getStatus()))
                .isPresent();
    }
}
