package com.badminton.mes.module.quality.convert;

import java.util.List;
import java.util.Map;

import com.badminton.mes.module.quality.controller.vo.QualityInspectionPlanItemRespVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionPlanRespVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionPlanSaveReqVO;
import com.badminton.mes.module.quality.dal.entity.QualityInspectionItemEntity;
import com.badminton.mes.module.quality.dal.entity.QualityInspectionPlanEntity;
import com.badminton.mes.module.quality.dal.entity.QualityInspectionPlanItemEntity;

/**
 * 检验标准方案显式转换器。
 *
 * <p>方案主表字段与方案项规则快照分层转换。方案项中的标准值、上下限、必检标记和判定方式来自版本快照；
 * 项目编码、名称、值类型和单位则从当前检验项目主数据冗余展示，不代表方案生效时的历史值。</p>
 */
public final class QualityInspectionPlanConvert {

    /** 将方案请求中的可编辑主表字段复制到新实体，版本和状态由 Service 初始化。 */
    public static QualityInspectionPlanEntity toEntity(QualityInspectionPlanSaveReqVO request) {
        QualityInspectionPlanEntity entity = new QualityInspectionPlanEntity();
        copyEditableFields(request, entity);
        return entity;
    }

    /**
     * 覆盖方案主表可编辑字段，不修改版本号、状态、审核信息、审计字段和删除标记。
     *
     * <p>默认方案标记按布尔真值归一，避免将空值持久化为不确定状态。</p>
     */
    public static void copyEditableFields(QualityInspectionPlanSaveReqVO request,
                                          QualityInspectionPlanEntity entity) {
        entity.setPlanCode(request.getPlanCode());
        entity.setPlanName(request.getPlanName());
        entity.setProductId(request.getProductId());
        entity.setCustomerId(request.getCustomerId());
        entity.setInspectionType(request.getInspectionType());
        entity.setEffectiveDate(request.getEffectiveDate());
        entity.setDefaultFlag(Boolean.TRUE.equals(request.getDefaultFlag()));
        entity.setRemark(request.getRemark());
    }

    /**
     * 组装方案完整详情。
     *
     * <p>方案项顺序沿用调用方提供的快照顺序；项目主数据缺失时仍保留方案项及其规则快照，
     * 仅省略无法取得的当前项目冗余字段。</p>
     */
    public static QualityInspectionPlanRespVO toRespVO(
            QualityInspectionPlanEntity entity,
            List<QualityInspectionPlanItemEntity> planItems,
            Map<Long, QualityInspectionItemEntity> inspectionItemsById) {
        QualityInspectionPlanRespVO response = toSummaryRespVO(entity);
        response.setItems(planItems.stream()
                .map(planItem -> toItemRespVO(planItem, inspectionItemsById.get(planItem.getInspectionItemId())))
                .toList());
        return response;
    }

    /** 转换方案摘要，并显式返回空方案项列表，避免分页查询误触发明细加载。 */
    public static QualityInspectionPlanRespVO toSummaryRespVO(QualityInspectionPlanEntity entity) {
        QualityInspectionPlanRespVO response = new QualityInspectionPlanRespVO();
        response.setId(entity.getId());
        response.setPlanCode(entity.getPlanCode());
        response.setPlanName(entity.getPlanName());
        response.setProductId(entity.getProductId());
        response.setCustomerId(entity.getCustomerId());
        response.setInspectionType(entity.getInspectionType());
        response.setVersionNo(entity.getVersionNo());
        response.setPlanStatus(entity.getPlanStatus());
        response.setEffectiveDate(entity.getEffectiveDate());
        response.setDefaultFlag(entity.getDefaultFlag());
        response.setRemark(entity.getRemark());
        response.setCreateBy(entity.getCreateBy());
        response.setAuditBy(entity.getAuditBy());
        response.setAuditTime(entity.getAuditTime());
        response.setCreateTime(entity.getCreateTime());
        response.setUpdateTime(entity.getUpdateTime());
        response.setItems(List.of());
        return response;
    }

    /**
     * 将单个方案项规则快照与当前项目主数据合并为响应。
     *
     * <p>判定规则始终取方案项快照；项目描述字段仅在当前主数据仍可读取时冗余填充。</p>
     */
    private static QualityInspectionPlanItemRespVO toItemRespVO(
            QualityInspectionPlanItemEntity planItem,
            QualityInspectionItemEntity inspectionItem) {
        QualityInspectionPlanItemRespVO response = new QualityInspectionPlanItemRespVO();
        response.setId(planItem.getId());
        response.setInspectionItemId(planItem.getInspectionItemId());
        if (inspectionItem != null) {
            response.setItemCode(inspectionItem.getItemCode());
            response.setItemName(inspectionItem.getItemName());
            response.setValueType(inspectionItem.getValueType());
            response.setUnit(inspectionItem.getUnit());
        }
        response.setSortOrder(planItem.getSortOrder());
        response.setSampleQuantity(planItem.getSampleQuantity());
        response.setRequiredFlag(planItem.getRequiredFlag());
        response.setStandardValue(planItem.getStandardValue());
        response.setLowerLimit(planItem.getLowerLimit());
        response.setUpperLimit(planItem.getUpperLimit());
        response.setJudgmentMethod(planItem.getJudgmentMethod());
        return response;
    }

    /** 工具类不允许实例化。 */
    private QualityInspectionPlanConvert() {
    }
}
