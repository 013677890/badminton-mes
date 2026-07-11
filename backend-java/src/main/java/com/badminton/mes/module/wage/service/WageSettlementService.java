package com.badminton.mes.module.wage.service;

import java.util.List;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.wage.controller.vo.EmployeeWageSummaryRespVO;
import com.badminton.mes.module.wage.controller.vo.ProcessWageSummaryRespVO;
import com.badminton.mes.module.wage.controller.vo.WageSettlementActionReqVO;
import com.badminton.mes.module.wage.controller.vo.WageSettlementAdjustReqVO;
import com.badminton.mes.module.wage.controller.vo.WageSettlementAuditLogPageReqVO;
import com.badminton.mes.module.wage.controller.vo.WageSettlementAuditLogRespVO;
import com.badminton.mes.module.wage.controller.vo.WageSettlementCalculateReqVO;
import com.badminton.mes.module.wage.controller.vo.WageSettlementDetailPageReqVO;
import com.badminton.mes.module.wage.controller.vo.WageSettlementDetailRespVO;
import com.badminton.mes.module.wage.controller.vo.WageSettlementPageReqVO;
import com.badminton.mes.module.wage.controller.vo.WageSettlementRespVO;
import com.badminton.mes.module.wage.controller.vo.WageSettlementVersionReqVO;
import com.badminton.mes.module.wage.controller.vo.WageSummaryReqVO;

/** 计件工资结算服务。 */
public interface WageSettlementService {
    /** 计算新结算批次。 */
    Long calculateSettlement(WageSettlementCalculateReqVO reqVO);
    /** 按原范围重新计算草稿或已驳回批次。 */
    void recalculateSettlement(Long id, WageSettlementVersionReqVO reqVO);
    /** 提交结算审核。 */
    void submitSettlement(Long id, WageSettlementActionReqVO reqVO);
    /** 审核通过结算。 */
    void approveSettlement(Long id, WageSettlementActionReqVO reqVO);
    /** 驳回结算。 */
    void rejectSettlement(Long id, WageSettlementActionReqVO reqVO);
    /** 调整草稿明细最终金额。 */
    void adjustDetail(Long id, Long detailId, WageSettlementAdjustReqVO reqVO);
    /** 查询结算详情。 */
    WageSettlementRespVO getSettlement(Long id);
    /** 分页查询结算批次。 */
    PageResult<WageSettlementRespVO> getSettlementPage(WageSettlementPageReqVO reqVO);
    /** 分页查询当前有效结算明细。 */
    PageResult<WageSettlementDetailRespVO> getDetailPage(Long id, WageSettlementDetailPageReqVO reqVO);
    /** 分页查询结算审计日志。 */
    PageResult<WageSettlementAuditLogRespVO> getAuditLogPage(
            Long id, WageSettlementAuditLogPageReqVO reqVO);
    /** 按员工汇总已审核工资。 */
    List<EmployeeWageSummaryRespVO> summarizeEmployees(WageSummaryReqVO reqVO);
    /** 按工序汇总已审核工资。 */
    List<ProcessWageSummaryRespVO> summarizeProcesses(WageSummaryReqVO reqVO);
}
