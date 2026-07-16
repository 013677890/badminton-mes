package com.badminton.mes.module.device.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.device.controller.vo.DeviceCommissioningPageReqVO;
import com.badminton.mes.module.device.controller.vo.DeviceCommissioningRespVO;
import com.badminton.mes.module.device.controller.vo.DeviceCommissioningSaveReqVO;

/**
 * 设备联调记录 Service，由设备联调 Controller 调用，用于保存设备接入前的连通性、
 * 报文和结果记录；它不直接改变生产计数，正式上报仍通过 {@link DeviceCountService}。
 *
 * @author MES 开发组
 * @date 2026/07/16
 */
public interface DeviceCommissioningService {

    /** 创建一次设备联调记录。 */
    Long createCommissioningRecord(DeviceCommissioningSaveReqVO request);

    /** 查询单条设备联调记录。 */
    DeviceCommissioningRespVO getCommissioningRecord(Long id);

    /** 分页查询设备联调记录。 */
    PageResult<DeviceCommissioningRespVO> getCommissioningRecordPage(DeviceCommissioningPageReqVO request);
}
