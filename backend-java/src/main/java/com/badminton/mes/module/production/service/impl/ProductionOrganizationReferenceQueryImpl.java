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
        // 先锁车间，再锁产线，和组织写服务保持统一锁序；返回值只表示当前关系是否可用，
        // 不向调用方泄露实体，也不在这里修改任何组织数据。
        if (workshopId == null) {
            return lineId == null;
        }

        WorkshopEntity workshop = workshopRepository
                .findByIdAndDeletedFalseForUpdate(workshopId)
                .orElse(null);
        // 车间不存在、已逻辑删除或已停用，都不能作为用户生产组织归属。
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
        // 产线必须属于已锁定车间且处于启用状态，防止跨车间或停用产线被分配给用户。
        return line != null
                && CommonStatusEnum.ENABLED.getStatus().equals(line.getStatus())
                && workshopId.equals(line.getWorkshopId());
    }
}
