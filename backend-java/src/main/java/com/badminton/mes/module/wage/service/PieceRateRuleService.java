package com.badminton.mes.module.wage.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.wage.controller.vo.PieceRateRulePageReqVO;
import com.badminton.mes.module.wage.controller.vo.PieceRateRuleRespVO;
import com.badminton.mes.module.wage.controller.vo.PieceRateRuleSaveReqVO;
import com.badminton.mes.module.wage.controller.vo.PieceRateRuleStatusReqVO;
import com.badminton.mes.module.wage.controller.vo.PieceRateRuleUpdateReqVO;
import com.badminton.mes.module.wage.controller.vo.WageRuleChangeLogPageReqVO;
import com.badminton.mes.module.wage.controller.vo.WageRuleChangeLogRespVO;

/** 计件规则业务服务。 */
public interface PieceRateRuleService {
    /** 创建规则。 */
    Long createRule(PieceRateRuleSaveReqVO reqVO);
    /** 按预期版本修改规则。 */
    void updateRule(Long id, PieceRateRuleUpdateReqVO reqVO);
    /** 按预期版本删除规则。 */
    void deleteRule(Long id, Integer version);
    /** 按预期版本变更规则状态。 */
    void updateRuleStatus(Long id, PieceRateRuleStatusReqVO reqVO);
    /** 查询规则详情。 */
    PieceRateRuleRespVO getRule(Long id);
    /** 分页查询规则。 */
    PageResult<PieceRateRuleRespVO> getRulePage(PieceRateRulePageReqVO reqVO);
    /** 分页查询规则变更日志。 */
    PageResult<WageRuleChangeLogRespVO> getRuleChangeLogPage(
            Long id, WageRuleChangeLogPageReqVO reqVO);
}
