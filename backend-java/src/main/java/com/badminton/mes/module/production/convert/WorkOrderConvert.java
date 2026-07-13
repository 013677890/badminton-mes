package com.badminton.mes.module.production.convert;

import java.util.List;
import java.util.Map;

import com.badminton.mes.module.production.controller.vo.WorkOrderMaterialRespVO;
import com.badminton.mes.module.production.controller.vo.WorkOrderRespVO;
import com.badminton.mes.module.production.controller.vo.WorkOrderSaveReqVO;
import com.badminton.mes.module.production.controller.vo.WorkOrderStatusLogRespVO;
import com.badminton.mes.module.production.dal.entity.MaterialEntity;
import com.badminton.mes.module.production.dal.entity.WorkOrderEntity;
import com.badminton.mes.module.production.dal.entity.WorkOrderMaterialEntity;
import com.badminton.mes.module.production.dal.entity.WorkOrderStatusLogEntity;

/**
 * 生产工单 VO 与实体的转换器。
 *
 * <p>采用显式逐字段赋值：字段对应关系一目了然，编译期即可发现改名遗漏，
 * 也避免反射拷贝的性能损耗与浅拷贝陷阱(MISC-002 禁用 Apache BeanUtils)。
 *
 * @author 张竹灏
 * @date 2026/07/07
 */
public final class WorkOrderConvert {

    /**
     * 保存请求 VO 转实体，创建与修改共用。
     *
     * <p>只搬运计划字段。工单号、来源、状态、创建人与产品冗余字段
     * (productName/spec/unitId)由 Service 按业务规则另行设置。
     *
     * @param reqVO 保存请求 VO
     * @return 工单实体
     */
    public static WorkOrderEntity toEntity(WorkOrderSaveReqVO reqVO) {
        WorkOrderEntity workOrder = new WorkOrderEntity();
        workOrder.setProductId(reqVO.getProductId());
        workOrder.setBatchNo(reqVO.getBatchNo());
        workOrder.setBomId(reqVO.getBomId());
        workOrder.setRoutingId(reqVO.getRoutingId());
        workOrder.setCustomerId(reqVO.getCustomerId());
        workOrder.setWorkshopId(reqVO.getWorkshopId());
        workOrder.setPlanQuantity(reqVO.getPlanQuantity());
        workOrder.setOverRatio(reqVO.getOverRatio());
        workOrder.setPriority(reqVO.getPriority());
        workOrder.setPlanStartTime(reqVO.getPlanStartTime());
        workOrder.setPlanEndTime(reqVO.getPlanEndTime());
        return workOrder;
    }

    /**
     * 实体转响应 VO。
     *
     * @param workOrder 工单实体
     * @return 响应 VO
     */
    public static WorkOrderRespVO toRespVO(WorkOrderEntity workOrder) {
        WorkOrderRespVO respVO = new WorkOrderRespVO();
        respVO.setId(workOrder.getId());
        respVO.setWorkOrderNo(workOrder.getWorkOrderNo());
        respVO.setSourceType(workOrder.getSourceType());
        respVO.setSourceSystem(workOrder.getSourceSystem());
        respVO.setSourceOrderNo(workOrder.getSourceOrderNo());
        respVO.setProductId(workOrder.getProductId());
        respVO.setProductName(workOrder.getProductName());
        respVO.setSpec(workOrder.getSpec());
        respVO.setUnitId(workOrder.getUnitId());
        respVO.setBatchNo(workOrder.getBatchNo());
        respVO.setBomId(workOrder.getBomId());
        respVO.setRoutingId(workOrder.getRoutingId());
        respVO.setCustomerId(workOrder.getCustomerId());
        respVO.setWorkshopId(workOrder.getWorkshopId());
        respVO.setPlanQuantity(workOrder.getPlanQuantity());
        respVO.setDispatchedQuantity(workOrder.getDispatchedQuantity());
        respVO.setInputQuantity(workOrder.getInputQuantity());
        respVO.setFinishQuantity(workOrder.getFinishQuantity());
        respVO.setDefectQuantity(workOrder.getDefectQuantity());
        respVO.setReworkQuantity(workOrder.getReworkQuantity());
        respVO.setOverRatio(workOrder.getOverRatio());
        respVO.setPriority(workOrder.getPriority());
        respVO.setPlanStartTime(workOrder.getPlanStartTime());
        respVO.setPlanEndTime(workOrder.getPlanEndTime());
        respVO.setOrderStatus(workOrder.getOrderStatus());
        respVO.setKitStatus(workOrder.getKitStatus());
        respVO.setCreateTime(workOrder.getCreateTime());
        respVO.setUpdateTime(workOrder.getUpdateTime());
        return respVO;
    }

    /**
     * 实体列表转响应 VO 列表。
     *
     * @param list 工单实体列表
     * @return 响应 VO 列表；入参为空集合时返回空集合
     */
    public static List<WorkOrderRespVO> toRespVOList(List<WorkOrderEntity> list) {
        return list.stream().map(WorkOrderConvert::toRespVO).toList();
    }

    /**
     * 物料需求实体列表转响应 VO 列表，物料编码/名称按物料档案回填。
     *
     * @param list        物料需求实体列表
     * @param materialMap 物料档案，key 为物料 id；档案缺失时编码/名称为 null
     * @return 响应 VO 列表；入参为空集合时返回空集合
     */
    public static List<WorkOrderMaterialRespVO> toMaterialRespVOList(List<WorkOrderMaterialEntity> list,
                                                                     Map<Long, MaterialEntity> materialMap) {
        return list.stream().map(entity -> {
            WorkOrderMaterialRespVO respVO = new WorkOrderMaterialRespVO();
            respVO.setId(entity.getId());
            respVO.setWorkOrderId(entity.getWorkOrderId());
            respVO.setMaterialId(entity.getMaterialId());
            respVO.setRequireQuantity(entity.getRequireQuantity());
            respVO.setIssuedQuantity(entity.getIssuedQuantity());
            MaterialEntity material = materialMap.get(entity.getMaterialId());
            if (material != null) {
                respVO.setMaterialCode(material.getMaterialCode());
                respVO.setMaterialName(material.getMaterialName());
            }
            return respVO;
        }).toList();
    }

    /**
     * 状态日志实体列表转响应 VO 列表。
     *
     * @param list 状态日志实体列表
     * @return 响应 VO 列表；入参为空集合时返回空集合
     */
    public static List<WorkOrderStatusLogRespVO> toStatusLogRespVOList(List<WorkOrderStatusLogEntity> list) {
        return list.stream().map(entity -> {
            WorkOrderStatusLogRespVO respVO = new WorkOrderStatusLogRespVO();
            respVO.setId(entity.getId());
            respVO.setWorkOrderId(entity.getWorkOrderId());
            respVO.setFromStatus(entity.getFromStatus());
            respVO.setToStatus(entity.getToStatus());
            respVO.setChangeType(entity.getChangeType());
            respVO.setChangeReason(entity.getChangeReason());
            respVO.setOperateBy(entity.getOperateBy());
            respVO.setOperateTime(entity.getOperateTime());
            return respVO;
        }).toList();
    }

    private WorkOrderConvert() {
    }
}
