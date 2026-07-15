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
 * 设备接入配置持久化实体。
 *
 * <p>一条配置把外部采集点与 MES 设备台账、工序和产线建立稳定映射，并保存计数模式、跳变阈值、
 * 联调结论及正式采集开关。联调状态与启用状态是两个独立维度：只有联调通过后，业务层才允许启用采集。
 * 逻辑删除用于保留审计语义，历史计数和联调记录不得因配置停用而失去关联。
 */
@Data
@Entity
@DynamicInsert
@Table(name = "device_access_config")
public class DeviceAccessConfigEntity {

    /** 配置主键，也是联调记录和计数记录关联接入配置的稳定标识。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 对外暴露的接入配置编码，设备上报时据此定位采集规则。 */
    @Column(name = "config_code")
    private String configCode;

    /** 便于管理端识别配置用途的业务名称。 */
    @Column(name = "config_name")
    private String configName;

    /** 绑定的设备台账主键，用于核验上报设备身份及设备可用状态。 */
    @Column(name = "equipment_id")
    private Long equipmentId;

    /** 设备侧采集点编码；同一设备内应保持唯一，避免数据来源歧义。 */
    @Column(name = "collection_point_code")
    private String collectionPointCode;

    /** 计数数据归属工序；为空时上报数据会进入未配置工序异常。 */
    @Column(name = "process_id")
    private Long processId;

    /** 采集设备所属产线主键，供生产组织维度关联和筛选。 */
    @Column(name = "production_line_id")
    private Long productionLineId;

    /** 数据接入来源类型，当前由服务端固定为 HTTP API。 */
    @Column(name = "data_source")
    private String dataSource;

    /** 计数解释方式：累计值需与前序记录求差，增量值可直接作为本次产量。 */
    @Column(name = "count_mode")
    private String countMode;

    /** 单次有效增量上限；超过时保留原始增量并生成计数跳变异常。 */
    @Column(name = "spike_threshold")
    private Long spikeThreshold;

    /** 后续报工策略，决定匹配生产任务后是否自动报工、待确认或不报工。 */
    @Column(name = "report_mode")
    private String reportMode;

    /** 最近一次联调结论；未测试或失败的配置不得启用正式采集。 */
    @Column(name = "commissioning_status")
    private String commissioningStatus;

    /** 正式采集开关，1 表示允许接收计数，0 表示停用。 */
    @Column(name = "enabled_status")
    private Integer enabledStatus;

    /** 已接受报文中的最大采集时间，只允许单调向前更新，避免乱序报文回拨在线状态。 */
    @Column(name = "last_communication_time")
    private LocalDateTime lastCommunicationTime;

    /** 配置维护备注，不参与计数判定。 */
    @Column(name = "remark")
    private String remark;

    /** 创建配置的操作人主键；无登录上下文的内部调用使用系统默认操作人。 */
    @Column(name = "create_by")
    private Long createBy;

    /** 数据库生成的创建时间，仅用于读取和审计。 */
    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    /** 数据库维护的最近更新时间，仅用于读取和审计。 */
    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;

    /** 逻辑删除标记；被删除配置不再参与查询和上报，但物理记录仍保留。 */
    @Column(name = "is_deleted")
    private Boolean deleted;
}
