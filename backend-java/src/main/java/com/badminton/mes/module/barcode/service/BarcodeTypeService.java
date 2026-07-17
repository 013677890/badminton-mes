package com.badminton.mes.module.barcode.service;

import java.util.List;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.barcode.controller.vo.BarcodeTypePageReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeTypeRespVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeTypeSaveReqVO;

/**
 * 条码类型 Service，定义条码类型档案维护用例。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
public interface BarcodeTypeService {

    /**
     * 创建条码类型，新类型默认启用。
     *
     * @param reqVO 创建请求
     * @return 新类型主键 id
     */
    Long createBarcodeType(BarcodeTypeSaveReqVO reqVO);

    /**
     * 修改条码类型基础信息。
     *
     * @param id    类型主键
     * @param reqVO 修改请求
     */
    void updateBarcodeType(Long id, BarcodeTypeSaveReqVO reqVO);

    /**
     * 启用条码类型：停用 → 启用。
     *
     * @param id 类型主键
     */
    void enableBarcodeType(Long id);

    /**
     * 停用条码类型：启用 → 停用，停用后不允许新建相关应用规则。
     *
     * @param id 类型主键
     */
    void disableBarcodeType(Long id);

    /**
     * 删除条码类型(逻辑删除)，已被条码规则或应用规则使用时拒绝。
     *
     * @param id 类型主键
     */
    void deleteBarcodeType(Long id);

    /**
     * 查询条码类型详情。
     *
     * @param id 类型主键
     * @return 类型详情
     */
    BarcodeTypeRespVO getBarcodeType(Long id);

    /**
     * 分页查询条码类型。
     *
     * @param reqVO 分页筛选条件
     * @return 分页结果，无数据时 list 为空集合(API-002)
     */
    PageResult<BarcodeTypeRespVO> getBarcodeTypePage(BarcodeTypePageReqVO reqVO);

    /**
     * 查询启用状态的条码类型选项，编码升序。
     *
     * @return 启用类型列表，无数据时为空集合(API-002)
     */
    List<BarcodeTypeRespVO> getEnabledBarcodeTypeOptions();
}
