package com.badminton.mes.module.andon.service;

import java.util.List;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.andon.controller.vo.AndonEventActionReqVO;
import com.badminton.mes.module.andon.controller.vo.AndonEventCreateReqVO;
import com.badminton.mes.module.andon.controller.vo.AndonEventPageReqVO;
import com.badminton.mes.module.andon.controller.vo.AndonEventRespVO;
import com.badminton.mes.module.andon.controller.vo.AndonProcessLogRespVO;

/** 现场安灯异常 Service。 */
public interface AndonEventService {

    Long createEvent(AndonEventCreateReqVO request);

    void confirmEvent(Long id, AndonEventActionReqVO request);

    void startProcessing(Long id, AndonEventActionReqVO request);

    void transferEvent(Long id, AndonEventActionReqVO request);

    void completeEvent(Long id, AndonEventActionReqVO request);

    void closeEvent(Long id, AndonEventActionReqVO request);

    void escalateEvent(Long id, AndonEventActionReqVO request);

    int processTimeoutEvents();

    AndonEventRespVO getEvent(Long id);

    PageResult<AndonEventRespVO> getEventPage(AndonEventPageReqVO request);

    List<AndonProcessLogRespVO> getProcessLogs(Long id);
}
