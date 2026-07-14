package com.badminton.mes.module.andon.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.andon.controller.vo.AndonReasonPageReqVO;
import com.badminton.mes.module.andon.controller.vo.AndonReasonRespVO;
import com.badminton.mes.module.andon.controller.vo.AndonReasonSaveReqVO;

/** 安灯异常原因 Service。 */
public interface AndonReasonService {

    Long createReason(AndonReasonSaveReqVO request);

    void updateReason(Long id, AndonReasonSaveReqVO request);

    void deleteReason(Long id);

    AndonReasonRespVO getReason(Long id);

    PageResult<AndonReasonRespVO> getReasonPage(AndonReasonPageReqVO request);
}
