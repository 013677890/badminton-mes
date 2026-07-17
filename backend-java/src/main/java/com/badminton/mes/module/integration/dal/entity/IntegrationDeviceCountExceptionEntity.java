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
 * 设备计数异常池实体，对应 integration_device_count_exception。
 *
 * <p>保存设备计数无法入账的完整上下文、错误原因、处理状态和修正重试结果。原始请求快照与
 * 重试快照分开保存，保证人工修正不会覆盖第一次失败证据；异常池处理状态由 Service 在悲观锁
 * 保护下推进。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Data
@Entity(name = "IntegrationDeviceCountExceptionEntity")
@DynamicInsert
@Table(name = "integration_device_count_exception")
public class IntegrationDeviceCountExceptionEntity {

    /** 异常池自增主键。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 计数来源系统，参与异常幂等键。 */
    @Column(name = "source_system")
    private String sourceSystem;

    /** 来源系统内唯一消息键，异常首次写入和重试均沿用。 */
    @Column(name = "external_key")
    private String externalKey;

    /** 上报设备编码快照。 */
    @Column(name = "equipment_code")
    private String equipmentCode;

    /** 成功解析时的派工单主键；派工单不存在或状态异常时可为空。 */
    @Column(name = "dispatch_order_id")
    private Long dispatchOrderId;

    /** 外部上报的派工单号快照。 */
    @Column(name = "dispatch_no")
    private String dispatchNo;

    /** 成功解析时的工序主键；工序不存在时可为空。 */
    @Column(name = "process_id")
    private Long processId;

    /** 外部上报的工序编码快照。 */
    @Column(name = "process_code")
    private String processCode;

    /** 设备采集端业务时间。 */
    @Column(name = "collect_time")
    private LocalDateTime collectTime;

    /** 导致异常的原始累计计数值。 */
    @Column(name = "count_value")
    private Long countValue;

    /** 首次失败请求的 JSON 快照，作为不可覆盖的排障证据。 */
    @Column(name = "request_snapshot", columnDefinition = "json")
    private String requestSnapshot;

    /** 最后一次人工修正重试的 JSON 快照。 */
    @Column(name = "retry_request_snapshot", columnDefinition = "json")
    private String retryRequestSnapshot;

    /** 机器可识别的计数异常类型。 */
    @Column(name = "exception_type")
    private String exceptionType;

    /** 对外稳定业务错误码。 */
    @Column(name = "error_code")
    private String errorCode;

    /** 面向异常池和人工处理人员展示的错误原因。 */
    @Column(name = "error_message")
    private String errorMessage;

    /** 处理状态：0 待处理、1 修正处理成功、2 人工忽略。 */
    @Column(name = "handle_status")
    private Integer handleStatus;

    /** 最后推进异常处理状态的系统用户主键。 */
    @Column(name = "handle_by")
    private Long handleBy;

    /** 最后一次处理或忽略的时间。 */
    @Column(name = "handle_time")
    private LocalDateTime handleTime;

    /** 人工处理备注或忽略原因。 */
    @Column(name = "handle_remark")
    private String handleRemark;

    /** 修正重试对应的最终接口日志主键。 */
    @Column(name = "retry_log_id")
    private Long retryLogId;

    /** 修正重试成功后生成的设备计数记录主键。 */
    @Column(name = "retry_record_id")
    private Long retryRecordId;

    /** 首次接收异常消息的系统用户主键。 */
    @Column(name = "create_by")
    private Long createBy;

    /** 异常入池时间，由数据库维护。 */
    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    /** 异常最后更新时间，由数据库维护。 */
    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;

    /** 逻辑删除标记，异常池正常查询和处理锁均排除已删除数据。 */
    @Column(name = "is_deleted")
    private Boolean deleted;
}
