package com.badminton.mes.module.integration.dal.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.DynamicInsert;

import lombok.Data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 设备计数成功记录实体，对应 integration_device_count_record。
 *
 * <p>记录设备上报的累计值、相对上一条记录计算出的增量以及可选现场报工主键。来源系统、设备、
 * 派工单和工序共同定义累计基线；外部来源系统与幂等键则保证同一消息不会重复生成计数记录。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Data
@Entity(name = "IntegrationDeviceCountRecordEntity")
@DynamicInsert
@Table(name = "integration_device_count_record")
public class DeviceCountRecordEntity {

    /** 数据库自增主键，作为计数记录及自动报工来源的稳定标识。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 外部计数来源系统，参与业务幂等和累计基线分区。 */
    @Column(name = "source_system")
    private String sourceSystem;

    /** 来源系统内的唯一消息键，防止同一上报重复落库。 */
    @Column(name = "external_key")
    private String externalKey;

    /** 已规范化的设备业务编码。 */
    @Column(name = "equipment_code")
    private String equipmentCode;

    /** 计数归属的派工单主键。 */
    @Column(name = "dispatch_order_id")
    private Long dispatchOrderId;

    /** 计数归属的派工单号快照。 */
    @Column(name = "dispatch_no")
    private String dispatchNo;

    /** 计数归属的工序主键。 */
    @Column(name = "process_id")
    private Long processId;

    /** 计数归属的工序编码快照。 */
    @Column(name = "process_code")
    private String processCode;

    /** 设备采集端产生的业务时间。 */
    @Column(name = "collect_time")
    private LocalDateTime collectTime;

    /** 本次设备上报的累计计数值，作为下一次增量计算基线。 */
    @Column(name = "count_value")
    private Long countValue;

    /** 本次累计值减去最近有效累计值后的增量，用于现场报工。 */
    @Column(name = "increment_value")
    private Long incrementValue;

    /** 自动生成的现场报工主键，可为空表示未启用报工联动。 */
    @Column(name = "work_report_id")
    private Long workReportId;

    /** 接收该计数消息的系统用户主键。 */
    @Column(name = "create_by")
    private Long createBy;

    /** 记录创建时间，由数据库维护。 */
    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    /** 记录最后更新时间，由数据库维护。 */
    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;

    /** 逻辑删除标记，累计基线查询排除已删除记录。 */
    @Column(name = "is_deleted")
    private Boolean deleted;
}
