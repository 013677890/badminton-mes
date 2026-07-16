package com.badminton.mes.module.device.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.device.controller.vo.DeviceAccessConfigPageReqVO;
import com.badminton.mes.module.device.controller.vo.DeviceAccessConfigRespVO;
import com.badminton.mes.module.device.controller.vo.DeviceAccessConfigSaveReqVO;

/**
 * 设备接入配置 Service，由设备配置 Controller 调用；计数上报处理会读取这里的
 * 设备、产线和接入规则，因此修改配置时需要由实现类校验编码唯一性和启用状态。
 *
 * @author MES 开发组
 * @date 2026/07/16
 */
public interface DeviceAccessConfigService {

    /** 新增设备接入配置。 */
    Long createAccessConfig(DeviceAccessConfigSaveReqVO request);

    /** 修改设备接入配置。 */
    void updateAccessConfig(Long id, DeviceAccessConfigSaveReqVO request);

    /** 逻辑删除设备接入配置。 */
    void deleteAccessConfig(Long id);

    /** 查询单条设备接入配置。 */
    DeviceAccessConfigRespVO getAccessConfig(Long id);

    /** 按设备、产线和启用状态分页查询接入配置。 */
    PageResult<DeviceAccessConfigRespVO> getAccessConfigPage(DeviceAccessConfigPageReqVO request);
}
