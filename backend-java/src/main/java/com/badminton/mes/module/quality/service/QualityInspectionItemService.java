package com.badminton.mes.module.quality.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionItemPageReqVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionItemRespVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionItemSaveReqVO;

/** 检验项目 Service。 */
public interface QualityInspectionItemService {

    Long createItem(QualityInspectionItemSaveReqVO request);

    void updateItem(Long id, QualityInspectionItemSaveReqVO request);

    void deleteItem(Long id);

    QualityInspectionItemRespVO getItem(Long id);

    PageResult<QualityInspectionItemRespVO> getItemPage(QualityInspectionItemPageReqVO request);
}
