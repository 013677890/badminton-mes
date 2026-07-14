package com.badminton.mes.module.wage.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.wage.controller.vo.WageWorkRecordImportReqVO;
import com.badminton.mes.module.wage.controller.vo.WageWorkRecordImportRespVO;
import com.badminton.mes.module.wage.controller.vo.WageWorkRecordPageReqVO;
import com.badminton.mes.module.wage.controller.vo.WageWorkRecordRespVO;

/** 已审核报工计件快照服务。 */
public interface WageWorkRecordService {
    /** 幂等导入已审核报工。 */
    WageWorkRecordImportRespVO importRecords(WageWorkRecordImportReqVO reqVO);
    /** 分页查询计件报工快照。 */
    PageResult<WageWorkRecordRespVO> getRecordPage(WageWorkRecordPageReqVO reqVO);
}
