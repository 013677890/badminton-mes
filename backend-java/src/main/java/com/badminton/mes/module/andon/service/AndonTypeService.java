package com.badminton.mes.module.andon.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.andon.controller.vo.AndonTypePageReqVO;
import com.badminton.mes.module.andon.controller.vo.AndonTypeRespVO;
import com.badminton.mes.module.andon.controller.vo.AndonTypeSaveReqVO;

/**
 * 安灯异常类型档案 Service。
 *
 * <p>由 {@code AndonTypeController} 调用；安灯事件创建时根据类型读取处理模式、
 * 超时参数、灯控和默认责任角色，因此类型删除采用逻辑删除并保留历史可追溯性。
 *
 * @author MES 开发组
 * @date 2026/07/16
 */
public interface AndonTypeService {

    /** 新增安灯类型，并校验处理模式和超时配置的组合合法性。 */
    Long createType(AndonTypeSaveReqVO request);

    /** 修改安灯类型及其默认处理规则。 */
    void updateType(Long id, AndonTypeSaveReqVO request);

    /** 逻辑删除安灯类型；已存在历史事件时不得物理删除。 */
    void deleteType(Long id);

    /** 查询安灯类型详情。 */
    AndonTypeRespVO getType(Long id);

    /** 按关键字和启用状态分页查询安灯类型。 */
    PageResult<AndonTypeRespVO> getTypePage(AndonTypePageReqVO request);
}
