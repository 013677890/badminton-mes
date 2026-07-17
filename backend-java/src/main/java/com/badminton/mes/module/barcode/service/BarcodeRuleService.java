package com.badminton.mes.module.barcode.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.barcode.controller.vo.BarcodeRulePageReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeRulePreviewReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeRulePreviewRespVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeRuleRespVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeRuleSaveReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeRuleValidateReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeRuleValidateRespVO;

/**
 * 条码规则 Service，定义规则与组成明细维护、预览和校验用例。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
public interface BarcodeRuleService {

    /**
     * 创建条码规则及组成明细，新规则默认启用。
     *
     * @param reqVO 创建请求，含完整组成明细
     * @return 新规则主键 id
     */
    Long createBarcodeRule(BarcodeRuleSaveReqVO reqVO);

    /**
     * 修改条码规则并整体重写组成明细，只影响新生成条码，不影响历史条码。
     *
     * @param id    规则主键
     * @param reqVO 修改请求
     */
    void updateBarcodeRule(Long id, BarcodeRuleSaveReqVO reqVO);

    /**
     * 启用条码规则：停用 → 启用。
     *
     * @param id 规则主键
     */
    void enableBarcodeRule(Long id);

    /**
     * 停用条码规则：启用 → 停用。
     *
     * @param id 规则主键
     */
    void disableBarcodeRule(Long id);

    /**
     * 删除条码规则(逻辑删除)，已被应用规则引用或已产生流水的规则拒绝删除。
     *
     * @param id 规则主键
     */
    void deleteBarcodeRule(Long id);

    /**
     * 查询规则详情，含组成明细。
     *
     * @param id 规则主键
     * @return 规则详情
     */
    BarcodeRuleRespVO getBarcodeRule(Long id);

    /**
     * 分页查询条码规则，不含组成明细。
     *
     * @param reqVO 分页筛选条件
     * @return 分页结果，无数据时 list 为空集合(API-002)
     */
    PageResult<BarcodeRuleRespVO> getBarcodeRulePage(BarcodeRulePageReqVO reqVO);

    /**
     * 按提交配置预览生成效果，流水号取样例值 1，不落库、不消耗真实流水。
     *
     * @param reqVO 预览请求
     * @return 预览结果，含分段试算与容量
     */
    BarcodeRulePreviewRespVO previewBarcodeRule(BarcodeRulePreviewReqVO reqVO);

    /**
     * 校验规则配置合法性，返回逐条错误说明，不抛业务异常。
     *
     * @param reqVO 校验请求
     * @return 校验结果
     */
    BarcodeRuleValidateRespVO validateBarcodeRule(BarcodeRuleValidateReqVO reqVO);
}
