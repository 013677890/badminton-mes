package com.badminton.mes.module.device.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 设备计数原始记录详情响应。
 *
 * <p>同时暴露设备原始计数、服务端解释后的有效增量以及后续任务匹配和报工状态。
 * 原始快照用于审计，增量字段用于业务处理；两者不可互相替代，尤其在累计计数发生回退或跳变时。
 */
@Data
public class DeviceCountRecordRespVO {

    /** 计数记录主键。 */
    private Long id;
    /** 接收报文时命中的接入配置主键。 */
    private Long accessConfigId;
    /** 接收报文时绑定的设备台账主键。 */
    private Long equipmentId;
    /** 设备编码历史快照。 */
    private String equipmentCode;
    /** 采集点编码历史快照。 */
    private String collectionPointCode;

    /** 设备声明的采集业务时间；接入时校验不得晚于服务器当前时间，并参与幂等身份计算。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime collectedAt;

    /** 设备端流水号，参与幂等身份计算。 */
    private String serialNumber;
    /** 设备直接上报的计数值。 */
    private Long rawCount;
    /** 累计差分或增量解释后得到的本次数量。 */
    private Long incrementCount;
    /** 采集瞬间的运行状态快照：空闲、运行或停止；未上报时可为空。 */
    private String runtimeStatus;
    /** 采集瞬间的故障状态或故障码快照；设备未提供时可为空。 */
    private String faultStatus;
    /** 后续匹配到的生产任务主键；尚未匹配或异常时为空。 */
    private Long productionTaskId;
    /** 接收时配置关联的工序主键；配置未关联工序时为空并触发异常分流。 */
    private Long processId;
    /** 生产任务匹配状态：{@code PENDING} 待匹配、{@code MATCHED} 已匹配、{@code EXCEPTION} 异常。 */
    private String matchStatus;
    /** 生产报工单生成状态；计数刚接收入库时初始化为未创建。 */
    private String reportStatus;
    /** 设备原始报文，用于审计和协议排障。 */
    private String rawPayload;

    /** 服务端接收并持久化计数记录的时间，不等同于设备声明的采集时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
