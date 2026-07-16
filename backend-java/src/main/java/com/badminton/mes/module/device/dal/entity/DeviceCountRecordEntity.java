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
 * 设备计数原始记录持久化实体。
 *
 * <p>完整保存设备上报事实、配置快照、计算后的有效增量以及后续任务匹配和报工状态。
 * 原始值与计算值分开存储，便于追溯累计差分、计数回退和异常跳变；配置和设备编码采用快照字段，
 * 防止主数据后续修改改变历史记录的业务含义。去重键由配置、采集时间和设备流水号计算并受数据库唯一约束保护。
 */
@Data
@Entity
@DynamicInsert
@Table(name = "device_count_record")
public class DeviceCountRecordEntity {

    /** 计数记录主键，也是对应异常记录关联的来源标识。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 接收报文时命中的接入配置主键。 */
    @Column(name = "access_config_id")
    private Long accessConfigId;

    /** 接收报文时配置绑定的设备台账主键。 */
    @Column(name = "equipment_id")
    private Long equipmentId;

    /** 上报时的设备编码快照，避免台账改码影响历史追踪。 */
    @Column(name = "equipment_code_snapshot")
    private String equipmentCodeSnapshot;

    /** 上报时的采集点编码快照，保留数据来源的历史语义。 */
    @Column(name = "collection_point_code_snapshot")
    private String collectionPointCodeSnapshot;

    /** 设备声明的数据采集时间，也是累计模式选择前序记录的时间基准。 */
    @Column(name = "collected_at")
    private LocalDateTime collectedAt;

    /** 设备端流水号，与配置和采集时间共同构成业务幂等身份。 */
    @Column(name = "serial_number")
    private String serialNumber;

    /** 设备直接上报的原始计数；累计模式下不能直接作为本次产量。 */
    @Column(name = "raw_count")
    private Long rawCount;

    /** 经过累计差分或增量模式解释后的本次有效数量。 */
    @Column(name = "increment_count")
    private Long incrementCount;

    /** 报文携带的设备运行状态快照。 */
    @Column(name = "runtime_status")
    private String runtimeStatus;

    /** 报文携带的设备故障状态或故障码快照。 */
    @Column(name = "fault_status")
    private String faultStatus;

    /** 后续匹配到的生产任务主键；接收阶段通常为空。 */
    @Column(name = "production_task_id")
    private Long productionTaskId;

    /** 接收时配置关联的工序主键，用于后续生产任务匹配。 */
    @Column(name = "process_id")
    private Long processId;

    /** 生产任务匹配状态；业务异常时直接标记为异常，不进入正常待匹配流程。 */
    @Column(name = "match_status")
    private String matchStatus;

    /** 报工单生成状态；接收计数时初始化为未创建。 */
    @Column(name = "report_status")
    private String reportStatus;

    /** SHA-256 十六进制幂等键，数据库唯一约束负责封堵并发重复写入。 */
    @Column(name = "deduplication_key")
    private String deduplicationKey;

    /** 设备原始报文，用于审计、排障和协议问题复现。 */
    @Column(name = "raw_payload")
    private String rawPayload;

    /** 数据库生成的接收落库时间。 */
    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;
}
