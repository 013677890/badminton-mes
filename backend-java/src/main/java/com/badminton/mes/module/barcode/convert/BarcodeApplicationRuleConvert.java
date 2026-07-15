package com.badminton.mes.module.barcode.convert;

import java.util.List;

import com.badminton.mes.module.barcode.controller.vo.BarcodeApplicationRuleRespVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeApplicationRuleSaveReqVO;
import com.badminton.mes.module.barcode.dal.entity.BarcodeApplyRuleEntity;

/**
 * 条码应用规则 VO 与实体的转换器。
 *
 * <p>采用显式逐字段赋值：字段对应关系一目了然，编译期即可发现改名遗漏(MISC-002)。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
public final class BarcodeApplicationRuleConvert {

    /**
     * 保存请求 VO 转实体；状态、默认标记兜底值由 Service 按业务规则设置。
     *
     * @param reqVO 保存请求 VO
     * @return 应用规则实体
     */
    public static BarcodeApplyRuleEntity toEntity(BarcodeApplicationRuleSaveReqVO reqVO) {
        BarcodeApplyRuleEntity applyRule = new BarcodeApplyRuleEntity();
        applyRule.setObjectType(reqVO.getObjectType());
        applyRule.setProductId(reqVO.getProductId());
        applyRule.setMaterialId(reqVO.getMaterialId());
        applyRule.setBarcodeTypeId(reqVO.getBarcodeTypeId());
        applyRule.setBarcodeMode(reqVO.getBarcodeMode());
        applyRule.setRuleId(reqVO.getRuleId());
        applyRule.setTemplateId(reqVO.getTemplateId());
        applyRule.setSourceType(reqVO.getSourceType());
        applyRule.setDefaultFlag(reqVO.getDefaultFlag());
        return applyRule;
    }

    /**
     * 实体转响应 VO。
     *
     * @param applyRule 应用规则实体
     * @return 响应 VO
     */
    public static BarcodeApplicationRuleRespVO toRespVO(BarcodeApplyRuleEntity applyRule) {
        BarcodeApplicationRuleRespVO respVO = new BarcodeApplicationRuleRespVO();
        respVO.setId(applyRule.getId());
        respVO.setObjectType(applyRule.getObjectType());
        respVO.setProductId(applyRule.getProductId());
        respVO.setMaterialId(applyRule.getMaterialId());
        respVO.setBarcodeTypeId(applyRule.getBarcodeTypeId());
        respVO.setBarcodeMode(applyRule.getBarcodeMode());
        respVO.setRuleId(applyRule.getRuleId());
        respVO.setTemplateId(applyRule.getTemplateId());
        respVO.setSourceType(applyRule.getSourceType());
        respVO.setDefaultFlag(applyRule.getDefaultFlag());
        respVO.setVersion(applyRule.getVersion());
        respVO.setStatus(applyRule.getStatus());
        respVO.setCreateTime(applyRule.getCreateTime());
        respVO.setUpdateTime(applyRule.getUpdateTime());
        return respVO;
    }

    /**
     * 实体列表转响应 VO 列表。
     *
     * @param list 应用规则实体列表
     * @return 响应 VO 列表；入参为空集合时返回空集合
     */
    public static List<BarcodeApplicationRuleRespVO> toRespVOList(List<BarcodeApplyRuleEntity> list) {
        return list.stream().map(BarcodeApplicationRuleConvert::toRespVO).toList();
    }

    private BarcodeApplicationRuleConvert() {
    }
}
