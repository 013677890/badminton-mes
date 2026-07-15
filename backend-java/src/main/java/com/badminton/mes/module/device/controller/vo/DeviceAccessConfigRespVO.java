package com.badminton.mes.module.device.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 设备接入配置详情响应。
 *
 * <p>向管理端完整展示采集点与设备、工序、产线的绑定关系，以及联调、启用和最近通信状态。
 * 状态字段是管理端判断配置是否具备正式采集资格和设备通信是否活跃的主要依据。
 */
@Data
public class DeviceAccessConfigRespVO {

    /** 接入配置主键。 */
    private Long id;
    /** 设备上报时用于定位配置的唯一编码。 */
    private String configCode;
    /** 接入配置的业务名称。 */
    private String configName;
    /** 绑定的设备台账主键。 */
    private Long equipmentId;
    /** 设备内用于区分数据来源的采集点编码。 */
    private String collectionPointCode;
    /** 计数数据归属工序主键。 */
    private Long processId;
    /** 采集设备所属产线主键。 */
    private Long productionLineId;
    /** 数据来源类型；当前由服务端固定为 {@code HTTP_API}，表示通过 HTTP 接口接收报文。 */
    private String dataSource;
    /** 计数解释方式：{@code CUMULATIVE} 按历史原始值求差，{@code INCREMENTAL} 直接采用本次值。 */
    private String countMode;
    /** 判定单次计数异常跳变的增量上限。 */
    private Long spikeThreshold;
    /** 后续生产报工策略：自动报工、待人工确认或不生成报工。 */
    private String reportMode;
    /** 最近一次联调结论：未联调、通过或失败；该状态决定配置是否具备启用资格。 */
    private String commissioningStatus;
    /** 正式采集开关，1 为启用，0 为停用。 */
    private Integer enabledStatus;

    /** 已接收报文中的最大采集时间，乱序旧报文不会使其回退。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastCommunicationTime;

    /** 配置维护备注。 */
    private String remark;

    /** 配置首次持久化的服务端时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 配置业务字段最近一次持久化更新的服务端时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
