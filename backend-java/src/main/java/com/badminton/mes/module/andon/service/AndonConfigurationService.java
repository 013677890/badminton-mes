package com.badminton.mes.module.andon.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.andon.controller.vo.AndonConfigurationPageReqVO;
import com.badminton.mes.module.andon.controller.vo.AndonConfigurationRespVO;
import com.badminton.mes.module.andon.controller.vo.AndonConfigurationSaveReqVO;

/**
 * 安灯异常处理配置 Service。
 *
 * <p>配置用于把安灯类型映射到产线范围、处理人/角色、响应和升级时限以及通知渠道；
 * {@code AndonEventService} 创建异常时会优先读取产线配置，再回退到全局或类型默认值。
 * Controller 只负责参数校验和响应包装。
 *
 * @author MES 开发组
 * @date 2026/07/16
 */
public interface AndonConfigurationService {

    /** 新增一条安灯处理配置，并校验同一范围内的配置唯一性。 */
    Long createConfiguration(AndonConfigurationSaveReqVO request);

    /** 修改安灯处理配置；实现类负责保护已被事件使用的配置语义。 */
    void updateConfiguration(Long id, AndonConfigurationSaveReqVO request);

    /** 逻辑删除安灯处理配置。 */
    void deleteConfiguration(Long id);

    /** 查询单条安灯处理配置详情。 */
    AndonConfigurationRespVO getConfiguration(Long id);

    /** 按安灯类型、产线范围和启用状态分页查询配置。 */
    PageResult<AndonConfigurationRespVO> getConfigurationPage(
            AndonConfigurationPageReqVO request);
}
