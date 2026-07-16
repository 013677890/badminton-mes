package com.badminton.mes.module.andon.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.andon.controller.vo.AndonConfigurationPageReqVO;
import com.badminton.mes.module.andon.controller.vo.AndonConfigurationRespVO;
import com.badminton.mes.module.andon.controller.vo.AndonConfigurationSaveReqVO;

/**
 * 安灯协助处理配置维护契约。
 *
 * <p>配置以“安灯类型 + 作用范围”唯一：产线标识为空代表全局范围，具体产线配置覆盖全局配置。
 * 每条规则至少指定处理用户或处理角色；升级时限与升级责任主体必须成组出现，且升级时限必须晚于
 * 响应时限。存在活动事件时禁止修改或删除规则，避免事件处理中途改变责任和时限语义。
 */
public interface AndonConfigurationService {

    /**
     * 创建全局或产线级协助规则，未指定启用状态时按启用处理。
     *
     * @param request 作用范围、处理责任、响应/升级时限及通知渠道
     * @return 新配置主键
     * @throws com.badminton.mes.common.exception.ServiceException 类型不存在、规则不完整、责任主体不可用
     *         或同一作用范围重复时抛出
     */
    Long createConfiguration(AndonConfigurationSaveReqVO request);

    /**
     * 更新配置；安灯类型不可变，并要求该类型当前不存在活动事件。
     *
     * @param id 配置主键
     * @param request 更新后的完整规则
     * @throws com.badminton.mes.common.exception.ServiceException 配置或类型不存在、试图更换类型、存在活动事件、
     *         规则或责任主体无效，或作用范围重复时抛出
     */
    void updateConfiguration(Long id, AndonConfigurationSaveReqVO request);

    /**
     * 逻辑删除配置并释放原范围唯一键；存在活动事件时拒绝删除。
     *
     * @param id 配置主键
     * @throws com.badminton.mes.common.exception.ServiceException 配置不存在或对应类型仍有活动事件时抛出
     */
    void deleteConfiguration(Long id);

    /**
     * 查询配置详情并装配对应安灯类型信息。
     *
     * @param id 配置主键
     * @return 配置详情
     * @throws com.badminton.mes.common.exception.ServiceException 配置或关联类型不存在时抛出
     */
    AndonConfigurationRespVO getConfiguration(Long id);

    /**
     * 分页查询全局及产线级配置，并批量装配类型信息。
     *
     * @param request 筛选及分页条件
     * @return 配置分页结果
     * @throws com.badminton.mes.common.exception.ServiceException 结果中的关联类型缺失时抛出
     */
    PageResult<AndonConfigurationRespVO> getConfigurationPage(
            AndonConfigurationPageReqVO request);
}
