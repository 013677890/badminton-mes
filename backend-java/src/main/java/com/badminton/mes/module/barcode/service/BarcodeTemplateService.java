package com.badminton.mes.module.barcode.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.barcode.controller.vo.BarcodeTemplatePageReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeTemplatePreviewReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeTemplatePreviewRespVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeTemplateRespVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeTemplateSaveReqVO;

/**
 * 条码模板 Service，定义标签模板与字段配置维护、打印预览用例。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
public interface BarcodeTemplateService {

    /**
     * 创建条码模板及字段配置，初始版本 V1，默认启用。
     *
     * @param reqVO 创建请求，含完整字段配置
     * @return 新模板主键 id
     */
    Long createBarcodeTemplate(BarcodeTemplateSaveReqVO reqVO);

    /**
     * 修改条码模板：未被应用规则绑定时就地修改并重写字段；
     * 已被绑定时保留原版本行，生成升版本的新模板行(被绑定后修改需升版本)。
     *
     * @param id    模板主键(具体版本行)
     * @param reqVO 修改请求，模板编码被忽略
     */
    void updateBarcodeTemplate(Long id, BarcodeTemplateSaveReqVO reqVO);

    /**
     * 启用条码模板：停用 → 启用。
     *
     * @param id 模板主键
     */
    void enableBarcodeTemplate(Long id);

    /**
     * 停用条码模板：启用 → 停用。
     *
     * @param id 模板主键
     */
    void disableBarcodeTemplate(Long id);

    /**
     * 查询模板详情，含字段配置。
     *
     * @param id 模板主键
     * @return 模板详情
     */
    BarcodeTemplateRespVO getBarcodeTemplate(Long id);

    /**
     * 分页查询条码模板，不含字段配置。
     *
     * @param reqVO 分页筛选条件
     * @return 分页结果，无数据时 list 为空集合(API-002)
     */
    PageResult<BarcodeTemplateRespVO> getBarcodeTemplatePage(BarcodeTemplatePageReqVO reqVO);

    /**
     * 生成模板打印预览数据，第一阶段不驱动真实打印机(已冻结决策)。
     *
     * @param reqVO 预览请求
     * @return 预览数据：布局与逐字段展示内容
     */
    BarcodeTemplatePreviewRespVO previewBarcodeTemplate(BarcodeTemplatePreviewReqVO reqVO);
}
