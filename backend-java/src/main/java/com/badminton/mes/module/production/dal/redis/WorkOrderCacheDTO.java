package com.badminton.mes.module.production.dal.redis;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.badminton.mes.module.production.dal.entity.WorkOrderEntity;

import lombok.Data;

/**
 * 工单详情缓存 DTO。
 *
 * <p>Redis 缓存使用独立 DTO，避免把 JPA 运行期状态或未来实体关系写入缓存。
 *
 * @author 张竹灏
 * @date 2026/07/08
 */
@Data
public class WorkOrderCacheDTO {

    private Long id;
    private String workOrderNo;
    private Integer sourceType;
    private String sourceSystem;
    private String sourceOrderNo;
    private Long productId;
    private String productName;
    private String spec;
    private Long unitId;
    private String batchNo;
    private Long bomId;
    private Long routingId;
    private Long customerId;
    private Long workshopId;
    private Integer planQuantity;
    private Integer dispatchedQuantity;
    private Integer inputQuantity;
    private Integer finishQuantity;
    private Integer defectQuantity;
    private Integer reworkQuantity;
    private BigDecimal overRatio;
    private Integer priority;
    private LocalDateTime planStartTime;
    private LocalDateTime planEndTime;
    private Integer orderStatus;
    private Integer kitStatus;
    private Long createBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Boolean deleted;

    /**
     * 从实体构造缓存 DTO。
     *
     * @param entity 工单实体
     * @return 缓存 DTO
     */
    public static WorkOrderCacheDTO fromEntity(WorkOrderEntity entity) {
        // 显式复制字段，保证缓存结构随实体变化可控，并且不依赖 Bean 序列化对 JPA 代理的处理。
        WorkOrderCacheDTO dto = new WorkOrderCacheDTO();
        dto.setId(entity.getId());
        dto.setWorkOrderNo(entity.getWorkOrderNo());
        dto.setSourceType(entity.getSourceType());
        dto.setSourceSystem(entity.getSourceSystem());
        dto.setSourceOrderNo(entity.getSourceOrderNo());
        dto.setProductId(entity.getProductId());
        dto.setProductName(entity.getProductName());
        dto.setSpec(entity.getSpec());
        dto.setUnitId(entity.getUnitId());
        dto.setBatchNo(entity.getBatchNo());
        dto.setBomId(entity.getBomId());
        dto.setRoutingId(entity.getRoutingId());
        dto.setCustomerId(entity.getCustomerId());
        dto.setWorkshopId(entity.getWorkshopId());
        dto.setPlanQuantity(entity.getPlanQuantity());
        dto.setDispatchedQuantity(entity.getDispatchedQuantity());
        dto.setInputQuantity(entity.getInputQuantity());
        dto.setFinishQuantity(entity.getFinishQuantity());
        dto.setDefectQuantity(entity.getDefectQuantity());
        dto.setReworkQuantity(entity.getReworkQuantity());
        dto.setOverRatio(entity.getOverRatio());
        dto.setPriority(entity.getPriority());
        dto.setPlanStartTime(entity.getPlanStartTime());
        dto.setPlanEndTime(entity.getPlanEndTime());
        dto.setOrderStatus(entity.getOrderStatus());
        dto.setKitStatus(entity.getKitStatus());
        dto.setCreateBy(entity.getCreateBy());
        dto.setCreateTime(entity.getCreateTime());
        dto.setUpdateTime(entity.getUpdateTime());
        dto.setDeleted(entity.getDeleted());
        return dto;
    }

    /**
     * 转回工单实体供 Service 复用转换逻辑。
     *
     * @return 工单实体
     */
    public WorkOrderEntity toEntity() {
        // 回源命中缓存时重建普通实体；该对象只用于读取和 VO 转换，不应直接作为更新实体提交。
        WorkOrderEntity entity = new WorkOrderEntity();
        entity.setId(id);
        entity.setWorkOrderNo(workOrderNo);
        entity.setSourceType(sourceType);
        entity.setSourceSystem(sourceSystem);
        entity.setSourceOrderNo(sourceOrderNo);
        entity.setProductId(productId);
        entity.setProductName(productName);
        entity.setSpec(spec);
        entity.setUnitId(unitId);
        entity.setBatchNo(batchNo);
        entity.setBomId(bomId);
        entity.setRoutingId(routingId);
        entity.setCustomerId(customerId);
        entity.setWorkshopId(workshopId);
        entity.setPlanQuantity(planQuantity);
        entity.setDispatchedQuantity(dispatchedQuantity);
        entity.setInputQuantity(inputQuantity);
        entity.setFinishQuantity(finishQuantity);
        entity.setDefectQuantity(defectQuantity);
        entity.setReworkQuantity(reworkQuantity);
        entity.setOverRatio(overRatio);
        entity.setPriority(priority);
        entity.setPlanStartTime(planStartTime);
        entity.setPlanEndTime(planEndTime);
        entity.setOrderStatus(orderStatus);
        entity.setKitStatus(kitStatus);
        entity.setCreateBy(createBy);
        entity.setCreateTime(createTime);
        entity.setUpdateTime(updateTime);
        entity.setDeleted(deleted);
        return entity;
    }
}
