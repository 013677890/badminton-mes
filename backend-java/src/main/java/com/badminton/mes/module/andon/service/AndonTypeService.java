package com.badminton.mes.module.andon.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.andon.controller.vo.AndonTypePageReqVO;
import com.badminton.mes.module.andon.controller.vo.AndonTypeRespVO;
import com.badminton.mes.module.andon.controller.vo.AndonTypeSaveReqVO;

/** 安灯类型 Service。 */
public interface AndonTypeService {

    Long createType(AndonTypeSaveReqVO request);

    void updateType(Long id, AndonTypeSaveReqVO request);

    void deleteType(Long id);

    AndonTypeRespVO getType(Long id);

    PageResult<AndonTypeRespVO> getTypePage(AndonTypePageReqVO request);
}
