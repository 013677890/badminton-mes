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
 * @author 张竹灏
 * @date 2026/07/11
 */
@Data
@Entity
@DynamicInsert
@Table(name = "integration_write_log")
public class IntegrationWriteLogEntity {

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 接口类型 */
    @Column(name = "interface_type")
    private String interfaceType;

    /** 来源系统 */
    @Column(name = "source_system")
    private String sourceSystem;

    /** 来源侧业务键 */
    @Column(name = "business_key")
    private String businessKey;

    /** 请求 JSON 快照 */
    @Column(name = "request_snapshot", columnDefinition = "json")
    private String requestSnapshot;

    /** 写入状态 */
    @Column(name = "write_status")
    private Integer writeStatus;

    /** MES 业务主键 */
    @Column(name = "result_id")
    private Long resultId;

    /** MES 业务编号 */
    @Column(name = "result_no")
    private String resultNo;

    /** 失败错误码 */
    @Column(name = "error_code")
    private String errorCode;

    /** 失败原因 */
    @Column(name = "error_message")
    private String errorMessage;

    /** 调用用户 */
    @Column(name = "create_by")
    private Long createBy;

    /** 创建时间 */
    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;
}
