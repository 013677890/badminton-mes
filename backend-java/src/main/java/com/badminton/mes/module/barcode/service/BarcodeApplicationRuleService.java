package com.badminton.mes.module.barcode.service;

import java.util.List;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.barcode.controller.vo.BarcodeApplicationRuleOptionReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeApplicationRulePageReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeApplicationRuleRespVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeApplicationRuleSaveReqVO;

/**
 * 条码应用规则 Service，定义产品/物料与条码类型、规则、模板绑定关系的维护用例。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
public interface BarcodeApplicationRuleService {

    /**
     * 创建条码应用规则，新规则默认启用；同对象同类型仅一条启用默认规则。
     *
     * @param reqVO 创建请求
     * @return 新应用规则主键 id
     */
    Long createBarcodeApplicationRule(BarcodeApplicationRuleSaveReqVO reqVO);

    /**
     * 修改条码应用规则。
     *
     * @param id    应用规则主键
     * @param reqVO 修改请求
     */
    void updateBarcodeApplicationRule(Long id, BarcodeApplicationRuleSaveReqVO reqVO);

    /**
     * 启用应用规则：启用前校验条码类型、条码规则和标签模板均处于启用状态。
     *
     * @param id 应用规则主键
     */
    void enableBarcodeApplicationRule(Long id);

    /**
     * 停用应用规则：启用 → 停用。
     *
     * @param id 应用规则主键
     */
    void disableBarcodeApplicationRule(Long id);

    /**
     * 删除应用规则(逻辑删除)，已生成条码的应用规则拒绝删除。
     *
     * @param id 应用规则主键
     */
    void deleteBarcodeApplicationRule(Long id);

    /**
     * 查询应用规则详情。
     *
     * @param id 应用规则主键
     * @return 应用规则详情
     */
    BarcodeApplicationRuleRespVO getBarcodeApplicationRule(Long id);

    /**
     * 分页查询应用规则。
     *
     * @param reqVO 分页筛选条件
     * @return 分页结果，无数据时 list 为空集合(API-002)
     */
    PageResult<BarcodeApplicationRuleRespVO> getBarcodeApplicationRulePage(
            BarcodeApplicationRulePageReqVO reqVO);

    /**
     * 查询生成条码时可用的启用应用规则选项，默认规则在前。
     *
     * @param reqVO 选项过滤条件
     * @return 启用应用规则列表，无数据时为空集合(API-002)
     */
    List<BarcodeApplicationRuleRespVO> getBarcodeApplicationRuleOptions(
            BarcodeApplicationRuleOptionReqVO reqVO);
}
