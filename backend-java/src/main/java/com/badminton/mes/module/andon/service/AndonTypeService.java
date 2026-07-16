package com.badminton.mes.module.andon.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.andon.controller.vo.AndonTypePageReqVO;
import com.badminton.mes.module.andon.controller.vo.AndonTypeRespVO;
import com.badminton.mes.module.andon.controller.vo.AndonTypeSaveReqVO;

/**
 * 安灯类型维护契约。
 *
 * <p>类型定义异常类别、处理模式、默认响应规则、通知渠道和是否模拟联动设备灯。协助处理模式必须
 * 同时具备响应时限、责任角色和通知渠道；类型编码在未删除数据中保持唯一。被原因、配置或事件引用
 * 的类型禁止删除，更新类型后会级联失效依赖该类型展示信息的原因、配置和事件详情缓存。
 */
public interface AndonTypeService {

    /**
     * 创建安灯类型，补齐灯控和启用状态的默认值。
     *
     * @param request 类型定义及默认处理规则
     * @return 新类型主键
     * @throws com.badminton.mes.common.exception.ServiceException 类型编码重复或处理模式规则不完整时抛出
     */
    Long createType(AndonTypeSaveReqVO request);

    /**
     * 更新类型可编辑字段；请求未携带启用或灯控状态时保留原值。
     *
     * @param id 类型主键
     * @param request 更新内容
     * @throws com.badminton.mes.common.exception.ServiceException 类型不存在、编码重复或处理规则无效时抛出
     */
    void updateType(Long id, AndonTypeSaveReqVO request);

    /**
     * 逻辑删除完全未被引用的类型，并释放原业务编码。
     *
     * @param id 类型主键
     * @throws com.badminton.mes.common.exception.ServiceException 类型不存在、仍被业务数据引用或删除占位编码冲突时抛出
     */
    void deleteType(Long id);

    /**
     * 查询安灯类型详情，优先从详情缓存读取。
     *
     * @param id 类型主键
     * @return 类型详情
     * @throws com.badminton.mes.common.exception.ServiceException 类型不存在时抛出
     */
    AndonTypeRespVO getType(Long id);

    /**
     * 按条件分页查询类型，超出末页的页码会收敛到最后一页。
     *
     * @param request 筛选及分页条件
     * @return 类型分页结果
     */
    PageResult<AndonTypeRespVO> getTypePage(AndonTypePageReqVO request);
}
