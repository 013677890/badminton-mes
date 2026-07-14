package com.badminton.mes.module.device.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.device.controller.vo.DeviceAccessConfigPageReqVO;
import com.badminton.mes.module.device.controller.vo.DeviceAccessConfigRespVO;
import com.badminton.mes.module.device.controller.vo.DeviceAccessConfigSaveReqVO;

/** 设备接入配置 Service。 */
public interface DeviceAccessConfigService {

    Long createAccessConfig(DeviceAccessConfigSaveReqVO request);

    void updateAccessConfig(Long id, DeviceAccessConfigSaveReqVO request);

    void deleteAccessConfig(Long id);

    DeviceAccessConfigRespVO getAccessConfig(Long id);

    PageResult<DeviceAccessConfigRespVO> getAccessConfigPage(DeviceAccessConfigPageReqVO request);
}
