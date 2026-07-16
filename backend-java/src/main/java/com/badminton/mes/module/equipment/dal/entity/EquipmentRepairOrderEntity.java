package com.badminton.mes.module.equipment.dal.entity;

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
 * 设备报修任务持久化实体，逐字段映射数据库表 {@code equip_repair_order}。
 *
 * <p>一条记录贯穿故障上报、派工、维修和结束的完整状态流转，并沉淀维修结果作为设备履历。
 * 设备、故障原理和用户均以主键弱关联，不建立 JPA 级联；{@link DynamicInsert} 保留数据库默认值语义。
 *
 * @author 角色C
 * @date 2026/07/10
 */
@Data
@Entity
@DynamicInsert
@Table(name = "equip_repair_order")
public class EquipmentRepairOrderEntity {

    /** 映射主键列 {@code id}；数据库自增生成，作为报修状态流转和审计的稳定标识。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 映射 {@code repair_no}；报修任务的业务唯一单号，用于防重校验、展示和检索。 */
    @Column(name = "repair_no")
    private String repairNo;

    /** 映射 {@code equipment_id}；发生故障的设备台账主键。 */
    @Column(name = "equipment_id")
    private Long equipmentId;

    /** 映射 {@code fault_principle_id}；选用的标准故障原理主键，用于分类和处置指导。 */
    @Column(name = "fault_principle_id")
    private Long faultPrincipleId;

    /** 映射 {@code fault_description}；报修人记录的现场故障现象和补充情况。 */
    @Column(name = "fault_description")
    private String faultDescription;

    /** 映射 {@code report_time}；故障被正式上报的业务时间，也是列表时间范围筛选依据。 */
    @Column(name = "report_time")
    private LocalDateTime reportTime;

    /** 映射 {@code report_user_id}；提交报修的系统用户主键，用于责任追溯。 */
    @Column(name = "report_user_id")
    private Long reportUserId;

    /** 映射 {@code repair_user_id}；任务分派的维修人员用户主键，未派工时可为空。 */
    @Column(name = "repair_user_id")
    private Long repairUserId;

    /** 映射 {@code repair_start_time}；维修人员实际开始处理故障的时间。 */
    @Column(name = "repair_start_time")
    private LocalDateTime repairStartTime;

    /** 映射 {@code repair_end_time}；维修实际结束的时间，用于计算维修耗时。 */
    @Column(name = "repair_end_time")
    private LocalDateTime repairEndTime;

    /** 映射 {@code repair_result}；记录完成后的处理措施、验证结论等维修凭据。 */
    @Column(name = "repair_result")
    private String repairResult;

    /** 映射 {@code repair_status}；状态机值：REPORTED、ASSIGNED、REPAIRING、FINISHED 或 CANCELLED。 */
    @Column(name = "repair_status")
    private String repairStatus;

    /** 映射 {@code remark}；保存报修任务的非结构化补充说明。 */
    @Column(name = "remark")
    private String remark;

    /** 映射 {@code create_by}；创建报修任务的系统用户主键，供审计使用。 */
    @Column(name = "create_by")
    private Long createBy;

    /** 映射 {@code create_time}；数据库生成的记录创建时间，应用不写入也不更新。 */
    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    /** 映射 {@code update_time}；状态或维修信息最后变更时间，插入时可使用数据库默认值。 */
    @Column(name = "update_time", insertable = false)
    private LocalDateTime updateTime;

    /** 映射 {@code is_deleted}；逻辑删除标记，常规查询仅处理值为 {@code false} 的任务。 */
    @Column(name = "is_deleted")
    private Boolean deleted;
}
