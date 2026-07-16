package com.badminton.mes.module.device.dal.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.DynamicInsert;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 设备接入联调记录持久化实体。
 *
 * <p>记录一次针对接入配置的通信连通性、数据格式兼容性和综合测试结论，作为配置能否开启正式采集的依据。
 * 联调记录是不可替代的历史证据；新增失败记录会同步使配置停用，但不会删除此前联调事实。
 */
@Data
@Entity
@DynamicInsert
@Table(name = "device_commissioning_record")
public class DeviceCommissioningRecordEntity {

    /** 联调记录主键。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 被测试的设备接入配置主键。 */
    @Column(name = "access_config_id")
    private Long accessConfigId;

    /** 实际执行联调的业务时间，不允许晚于服务器当前时间。 */
    @Column(name = "test_time")
    private LocalDateTime testTime;

    /** 执行联调的用户主键，用于责任追踪。 */
    @Column(name = "tester_user_id")
    private Long testerUserId;

    /** 通信链路检查结果，例如成功建立连接或通信失败。 */
    @Column(name = "communication_result")
    private String communicationResult;

    /** 上报字段、编码和结构是否符合约定的数据格式检查结果。 */
    @Column(name = "data_format_result")
    private String dataFormatResult;

    /** 本次联调综合结论；只有通过才能使配置具备后续启用资格。 */
    @Column(name = "test_result")
    private String testResult;

    /** 失败原因及排障信息；任一检查失败时必须提供。 */
    @Column(name = "issue_description")
    private String issueDescription;

    /** 联调期间采集的样例报文，供接口对照和问题复现。 */
    @Column(name = "sample_payload")
    private String samplePayload;

    /** 创建该联调记录的操作人主键，通常与测试人一致。 */
    @Column(name = "create_by")
    private Long createBy;

    /** 数据库生成的记录创建时间。 */
    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;
}
