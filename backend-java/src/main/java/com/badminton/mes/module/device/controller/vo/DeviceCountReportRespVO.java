package com.badminton.mes.module.device.controller.vo;

import lombok.Data;

/**
 * 设备计数上报处理结果。
 *
 * <p>用于立即告知调用方本次报文对应的记录标识、解释后的有效增量，以及数据是否进入正常待匹配
 * 或异常处理流程。成功接收只代表原始计数已落库，不等同于已经匹配生产任务或完成报工。
 */
@Data
public class DeviceCountReportRespVO {

    /** 本次上报生成的计数记录主键。 */
    private Long countRecordId;
    /** 按配置计数模式计算后的本次有效数量。 */
    private Long incrementCount;
    /** 当前生产任务匹配状态；正常接收为待匹配，异常分流为异常。 */
    private String matchStatus;
    /** 当前生产报工单生成状态；接入阶段尚未创建报工单。 */
    private String reportStatus;
    /** 机器可识别的异常类型；正常接收并进入待匹配流程时为空。 */
    private String exceptionType;
    /** 面向调用方的后续处理或异常说明。 */
    private String processingMessage;
}
