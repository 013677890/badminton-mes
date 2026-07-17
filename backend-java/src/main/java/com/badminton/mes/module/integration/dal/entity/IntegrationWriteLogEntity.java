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
 * 外部接口写入日志实体，对应 integration_write_log。
 *
 * <p>保存外部请求快照、处理状态、MES 结果定位和错误信息。除审计用途外，部分接口还依赖日志
 * 作为幂等索引；因此相同来源业务键的失败、成功和重复处理结果都必须可追溯。
 *
 * @author 张竹灏
 * @date 2026/07/11
 */
@Data
@Entity
@DynamicInsert
@Table(name = "integration_write_log")
public class IntegrationWriteLogEntity {

    /** 接口日志自增主键，作为外部调用结果的审计引用。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 集成接口类型，隔离单位、工单、派工、计数和 ERP 同步日志。 */
    @Column(name = "interface_type")
    private String interfaceType;

    /** 发起写入的外部系统，按协议规范化保存。 */
    @Column(name = "source_system")
    private String sourceSystem;

    /** 来源系统业务键，用于幂等查询和审计定位。 */
    @Column(name = "business_key")
    private String businessKey;

    /** 接口收到的 JSON 请求快照，保留外部原始字段。 */
    @Column(name = "request_snapshot", columnDefinition = "json")
    private String requestSnapshot;

    /** 写入状态：成功、失败或重复。 */
    @Column(name = "write_status")
    private Integer writeStatus;

    /** 处理结果对应的 MES 业务主键，失败且未生成业务数据时可为空。 */
    @Column(name = "result_id")
    private Long resultId;

    /** 处理结果对应的 MES 业务编号快照。 */
    @Column(name = "result_no")
    private String resultNo;

    /** 失败时的稳定业务错误码，成功和无错误重复结果为空。 */
    @Column(name = "error_code")
    private String errorCode;

    /** 面向调用方和审计人员的失败原因。 */
    @Column(name = "error_message")
    private String errorMessage;

    /** 创建该接口日志的认证用户主键，不接受外部请求伪造。 */
    @Column(name = "create_by")
    private Long createBy;

    /** 日志创建时间，由数据库维护。 */
    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    /** 日志最后更新时间，由数据库维护。 */
    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;
}
