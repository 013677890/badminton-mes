package com.badminton.mes.module.andon.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.andon.controller.vo.AndonReasonPageReqVO;
import com.badminton.mes.module.andon.controller.vo.AndonReasonRespVO;
import com.badminton.mes.module.andon.controller.vo.AndonReasonSaveReqVO;

/**
 * 安灯异常原因档案 Service。
 *
 * <p>由 {@code AndonReasonController} 调用，负责原因与安灯类型的归属校验、
 * 启停状态和逻辑删除；安灯事件 Service 在创建或处理异常时读取本档案。
 *
 * @author MES 开发组
 * @date 2026/07/16
 */
public interface AndonReasonService {

    /** 新增异常原因档案并校验其所属安灯类型有效。 */
    Long createReason(AndonReasonSaveReqVO request);

    /** 修改异常原因档案；已被业务引用的关键字段按实现类规则保护。 */
    void updateReason(Long id, AndonReasonSaveReqVO request);

    /** 逻辑删除异常原因档案，避免破坏历史安灯事件引用。 */
    void deleteReason(Long id);

    /** 查询单条异常原因详情。 */
    AndonReasonRespVO getReason(Long id);

    /** 按类型、启用状态等条件分页查询异常原因。 */
    PageResult<AndonReasonRespVO> getReasonPage(AndonReasonPageReqVO request);
}
