package com.badminton.mes.module.production.convert;

import com.badminton.mes.module.production.controller.vo.ProductionLineRespVO;
import com.badminton.mes.module.production.controller.vo.ProductionLineSaveReqVO;
import com.badminton.mes.module.production.controller.vo.WorkshopRespVO;
import com.badminton.mes.module.production.controller.vo.WorkshopSaveReqVO;
import com.badminton.mes.module.production.dal.entity.ProductionLineEntity;
import com.badminton.mes.module.production.dal.entity.WorkshopEntity;

/**
 * 车间与产线基础资料对象转换器。
 *
 * @author 张竹灏
 * @date 2026/07/12
 */
public final class ProductionOrganizationConvert {

    /**
     * 创建车间实体。
     *
     * @param reqVO 车间保存请求
     * @return 车间实体
     */
    public static WorkshopEntity toWorkshopEntity(WorkshopSaveReqVO reqVO) {
        WorkshopEntity entity = new WorkshopEntity();
        copyWorkshop(reqVO, entity);
        return entity;
    }

    /**
     * 复制车间请求字段。
     *
     * @param reqVO 车间保存请求
     * @param entity 车间实体
     */
    public static void copyWorkshop(WorkshopSaveReqVO reqVO, WorkshopEntity entity) {
        entity.setWorkshopCode(reqVO.getWorkshopCode());
        entity.setWorkshopName(reqVO.getWorkshopName());
        entity.setManagerId(reqVO.getManagerId());
        entity.setStatus(reqVO.getStatus());
    }

    /**
     * 转换车间响应并回填主管姓名。
     *
     * @param entity 车间实体
     * @param managerName 车间主管姓名，可空
     * @return 车间响应
     */
    public static WorkshopRespVO toWorkshopRespVO(WorkshopEntity entity, String managerName) {
        WorkshopRespVO result = new WorkshopRespVO();
        result.setId(entity.getId());
        result.setWorkshopCode(entity.getWorkshopCode());
        result.setWorkshopName(entity.getWorkshopName());
        result.setManagerId(entity.getManagerId());
        result.setManagerName(managerName);
        result.setStatus(entity.getStatus());
        result.setVersion(entity.getVersion());
        result.setCreateTime(entity.getCreateTime());
        result.setUpdateTime(entity.getUpdateTime());
        return result;
    }

    /**
     * 创建产线实体。
     *
     * @param reqVO 产线保存请求
     * @return 产线实体
     */
    public static ProductionLineEntity toProductionLineEntity(ProductionLineSaveReqVO reqVO) {
        ProductionLineEntity entity = new ProductionLineEntity();
        copyProductionLine(reqVO, entity);
        return entity;
    }

    /**
     * 复制产线请求字段。
     *
     * @param reqVO 产线保存请求
     * @param entity 产线实体
     */
    public static void copyProductionLine(
            ProductionLineSaveReqVO reqVO, ProductionLineEntity entity) {
        entity.setLineCode(reqVO.getLineCode());
        entity.setLineName(reqVO.getLineName());
        entity.setWorkshopId(reqVO.getWorkshopId());
        entity.setStandardCapacity(reqVO.getStandardCapacity());
        entity.setStatus(reqVO.getStatus());
    }

    /**
     * 转换产线响应并回填车间展示字段。
     *
     * @param entity 产线实体
     * @param workshop 所属车间，可空
     * @return 产线响应
     */
    public static ProductionLineRespVO toProductionLineRespVO(
            ProductionLineEntity entity, WorkshopEntity workshop) {
        ProductionLineRespVO result = new ProductionLineRespVO();
        result.setId(entity.getId());
        result.setLineCode(entity.getLineCode());
        result.setLineName(entity.getLineName());
        result.setWorkshopId(entity.getWorkshopId());
        if (workshop != null) {
            result.setWorkshopCode(workshop.getWorkshopCode());
            result.setWorkshopName(workshop.getWorkshopName());
        }
        result.setStandardCapacity(entity.getStandardCapacity());
        result.setStatus(entity.getStatus());
        result.setVersion(entity.getVersion());
        result.setCreateTime(entity.getCreateTime());
        result.setUpdateTime(entity.getUpdateTime());
        return result;
    }

    private ProductionOrganizationConvert() {
    }
}
