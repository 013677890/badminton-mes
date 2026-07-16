package com.badminton.mes.module.quality.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionItemPageReqVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionItemRespVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionItemSaveReqVO;

/**
 * 质量检验项目档案 Service，由方案管理 Controller 调用，并被方案明细引用。
 *
 * @author MES 开发组
 * @date 2026/07/16
 */
public interface QualityInspectionItemService {

    /** 新增检验项目定义。 */
    Long createItem(QualityInspectionItemSaveReqVO request);

    /** 修改检验项目定义。 */
    void updateItem(Long id, QualityInspectionItemSaveReqVO request);

    /** 逻辑删除检验项目，保留历史检验记录可追溯性。 */
    void deleteItem(Long id);

    /** 查询单条检验项目详情。 */
    QualityInspectionItemRespVO getItem(Long id);

    /** 按关键字和启用状态分页查询检验项目。 */
    PageResult<QualityInspectionItemRespVO> getItemPage(QualityInspectionItemPageReqVO request);
}
