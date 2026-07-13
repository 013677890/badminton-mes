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
 * @author 张竹灏
 * @date 2026/07/13
 */
@Data
@Entity
@DynamicInsert
@Table(name = "integration_device_count_record")
public class DeviceCountRecordEntity {

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 来源系统 */
    @Column(name = "source_system")
    private String sourceSystem;

    /** 来源系统内幂等键 */
    @Column(name = "external_key")
    private String externalKey;

    /** 设备编码 */
    @Column(name = "equipment_code")
    private String equipmentCode;

    /** 匹配派工单主键 */
    @Column(name = "dispatch_order_id")
    private Long dispatchOrderId;

    /** 匹配派工单号 */
    @Column(name = "dispatch_no")
    private String dispatchNo;

    /** 匹配工序主键 */
    @Column(name = "process_id")
    private Long processId;

    /** 匹配工序编码 */
    @Column(name = "process_code")
    private String processCode;

    /** 设备采集时间 */
    @Column(name = "collect_time")
    private LocalDateTime collectTime;

    /** 设备累计计数值 */
    @Column(name = "count_value")
    private Long countValue;

    /** 相对最近记录的增量值 */
    @Column(name = "increment_value")
    private Long incrementValue;

    /** 生成的现场报工主键 */
    @Column(name = "work_report_id")
    private Long workReportId;

    /** 调用用户 */
    @Column(name = "create_by")
    private Long createBy;

    /** 创建时间 */
    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    /** 更新时间 */
    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @Column(name = "is_deleted")
    private Boolean deleted;
}
