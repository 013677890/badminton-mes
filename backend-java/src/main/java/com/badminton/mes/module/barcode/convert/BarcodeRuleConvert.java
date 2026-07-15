package com.badminton.mes.module.barcode.convert;

import java.util.List;

import com.badminton.mes.module.barcode.controller.vo.BarcodeRuleItemRespVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeRuleItemSaveReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeRuleRespVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeRuleSaveReqVO;
import com.badminton.mes.module.barcode.dal.entity.BarcodeRuleEntity;
import com.badminton.mes.module.barcode.dal.entity.BarcodeRuleItemEntity;

/**
 * 条码规则 VO 与实体的转换器。
 *
 * <p>采用显式逐字段赋值：字段对应关系一目了然，编译期即可发现改名遗漏(MISC-002)。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
public final class BarcodeRuleConvert {

    /**
     * 保存请求 VO 转规则实体，创建与修改共用；状态由 Service 按业务规则另行设置。
     *
     * @param reqVO 保存请求 VO
     * @return 规则实体
     */
    public static BarcodeRuleEntity toEntity(BarcodeRuleSaveReqVO reqVO) {
        BarcodeRuleEntity rule = new BarcodeRuleEntity();
        rule.setRuleCode(reqVO.getRuleCode());
        rule.setRuleName(reqVO.getRuleName());
        rule.setBarcodeTypeId(reqVO.getBarcodeTypeId());
        rule.setSerialLength(reqVO.getSerialLength());
        rule.setSerialResetCycle(reqVO.getSerialResetCycle());
        return rule;
    }

    /**
     * 组成项请求列表转明细实体列表。
     *
     * @param ruleId 所属规则主键
     * @param items  组成项请求列表
     * @return 明细实体列表
     */
    public static List<BarcodeRuleItemEntity> toItemEntities(Long ruleId,
                                                             List<BarcodeRuleItemSaveReqVO> items) {
        return items.stream().map(item -> {
            BarcodeRuleItemEntity entity = new BarcodeRuleItemEntity();
            entity.setRuleId(ruleId);
            entity.setSeq(item.getSeq());
            entity.setItemType(item.getItemType());
            entity.setItemValue(item.getItemValue());
            entity.setDateFormat(item.getDateFormat());
            entity.setItemLength(item.getItemLength());
            return entity;
        }).toList();
    }

    /**
     * 规则实体转响应 VO，不含组成明细(分页列表使用)。
     *
     * @param rule 规则实体
     * @return 响应 VO
     */
    public static BarcodeRuleRespVO toRespVO(BarcodeRuleEntity rule) {
        BarcodeRuleRespVO respVO = new BarcodeRuleRespVO();
        respVO.setId(rule.getId());
        respVO.setRuleCode(rule.getRuleCode());
        respVO.setRuleName(rule.getRuleName());
        respVO.setBarcodeTypeId(rule.getBarcodeTypeId());
        respVO.setSerialLength(rule.getSerialLength());
        respVO.setSerialResetCycle(rule.getSerialResetCycle());
        respVO.setStatus(rule.getStatus());
        respVO.setCreateTime(rule.getCreateTime());
        respVO.setUpdateTime(rule.getUpdateTime());
        return respVO;
    }

    /**
     * 规则实体与组成明细转详情响应 VO。
     *
     * @param rule  规则实体
     * @param items 组成明细列表，按 seq 升序
     * @return 详情响应 VO
     */
    public static BarcodeRuleRespVO toRespVO(BarcodeRuleEntity rule, List<BarcodeRuleItemEntity> items) {
        BarcodeRuleRespVO respVO = toRespVO(rule);
        respVO.setItems(toItemRespVOList(items));
        return respVO;
    }

    /**
     * 规则实体列表转响应 VO 列表。
     *
     * @param list 规则实体列表
     * @return 响应 VO 列表；入参为空集合时返回空集合
     */
    public static List<BarcodeRuleRespVO> toRespVOList(List<BarcodeRuleEntity> list) {
        return list.stream().map(BarcodeRuleConvert::toRespVO).toList();
    }

    /**
     * 明细实体列表转响应 VO 列表。
     *
     * @param items 明细实体列表
     * @return 响应 VO 列表；入参为空集合时返回空集合
     */
    public static List<BarcodeRuleItemRespVO> toItemRespVOList(List<BarcodeRuleItemEntity> items) {
        return items.stream().map(item -> {
            BarcodeRuleItemRespVO respVO = new BarcodeRuleItemRespVO();
            respVO.setId(item.getId());
            respVO.setSeq(item.getSeq());
            respVO.setItemType(item.getItemType());
            respVO.setItemValue(item.getItemValue());
            respVO.setDateFormat(item.getDateFormat());
            respVO.setItemLength(item.getItemLength());
            return respVO;
        }).toList();
    }

    private BarcodeRuleConvert() {
    }
}
