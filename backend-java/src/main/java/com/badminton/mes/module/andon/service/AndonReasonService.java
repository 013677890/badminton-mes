package com.badminton.mes.module.andon.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.andon.controller.vo.AndonReasonPageReqVO;
import com.badminton.mes.module.andon.controller.vo.AndonReasonRespVO;
import com.badminton.mes.module.andon.controller.vo.AndonReasonSaveReqVO;

/**
 * 安灯异常原因维护契约。
 *
 * <p>每个原因必须归属于一个有效安灯类型，原因编码在未删除数据中保持唯一。已有事件引用的原因
 * 不允许跨类型调整或删除，以保证历史事件的原因归属可追溯；更新和删除会在事务提交后失效详情缓存。
 */
public interface AndonReasonService {

    /**
     * 创建原因，未指定启用状态时按启用处理。
     *
     * @param request 原因编码、名称、所属类型及状态
     * @return 新原因主键
     * @throws com.badminton.mes.common.exception.ServiceException 所属类型不存在或原因编码重复时抛出
     */
    Long createReason(AndonReasonSaveReqVO request);

    /**
     * 更新原因可编辑字段；跨类型变更会同时锁定新旧类型并检查历史事件引用。
     *
     * @param id 原因主键
     * @param request 更新内容
     * @throws com.badminton.mes.common.exception.ServiceException 原因或类型不存在、编码重复，或已被事件引用
     *         的原因试图改变所属类型时抛出
     */
    void updateReason(Long id, AndonReasonSaveReqVO request);

    /**
     * 逻辑删除未被事件引用的原因，并释放原业务编码供后续使用。
     *
     * @param id 原因主键
     * @throws com.badminton.mes.common.exception.ServiceException 原因不存在、仍被事件引用或删除占位编码冲突时抛出
     */
    void deleteReason(Long id);

    /**
     * 查询原因详情并装配所属安灯类型的编码和名称。
     *
     * @param id 原因主键
     * @return 原因详情
     * @throws com.badminton.mes.common.exception.ServiceException 原因或所属类型不存在时抛出
     */
    AndonReasonRespVO getReason(Long id);

    /**
     * 分页查询原因并批量装配类型信息，避免逐条查询关联类型。
     *
     * @param request 筛选及分页条件
     * @return 原因分页结果
     * @throws com.badminton.mes.common.exception.ServiceException 结果中的所属类型缺失时抛出
     */
    PageResult<AndonReasonRespVO> getReasonPage(AndonReasonPageReqVO request);
}
