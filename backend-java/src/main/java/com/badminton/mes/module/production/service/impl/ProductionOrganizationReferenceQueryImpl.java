package com.badminton.mes.module.production.service.impl;

import com.badminton.mes.common.enums.CommonStatusEnum;
import com.badminton.mes.module.production.dal.entity.ProductionLineEntity;
import com.badminton.mes.module.production.dal.entity.WorkshopEntity;
import com.badminton.mes.module.production.dal.repository.ProductionLineRepository;
import com.badminton.mes.module.production.dal.repository.WorkshopRepository;
import com.badminton.mes.module.production.service.ProductionOrganizationReferenceQuery;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 生产组织引用查询实现，通过统一锁序保证用户归属与组织启停互斥。
 *
 * @author 张竹灏
 * @date 2026/07/12
 */
@Service
public class ProductionOrganizationReferenceQueryImpl
        implements ProductionOrganizationReferenceQuery {

    private final WorkshopRepository workshopRepository;

    private final ProductionLineRepository productionLineRepository;

    /**
     * 构造生产组织引用查询服务。
     *
     * @param workshopRepository 车间 Repository
     * @param productionLineRepository 产线 Repository
     */
    public ProductionOrganizationReferenceQueryImpl(
            WorkshopRepository workshopRepository,
            ProductionLineRepository productionLineRepository) {
        this.workshopRepository = workshopRepository;
        this.productionLineRepository = productionLineRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean lockAndCheckAssignment(Long workshopId, Long lineId) {
        if (workshopId == null) {
            return lineId == null;
        }

        WorkshopEntity workshop = workshopRepository
                .findByIdAndDeletedFalseForUpdate(workshopId)
                .orElse(null);
        if (workshop == null
                || !CommonStatusEnum.ENABLED.getStatus().equals(workshop.getStatus())) {
            return false;
        }
        if (lineId == null) {
            return true;
        }

        ProductionLineEntity line = productionLineRepository
                .findByIdAndDeletedFalseForUpdate(lineId)
                .orElse(null);
        return line != null
                && CommonStatusEnum.ENABLED.getStatus().equals(line.getStatus())
                && workshopId.equals(line.getWorkshopId());
    }
}
