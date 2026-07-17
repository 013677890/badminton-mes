package com.badminton.mes.module.equipment.controller.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 设备台账响应 VO，设备详情与分页列表共用。
 *
 * <p>返回设备身份、制造信息、组织归属、日期和状态等业务展示字段，不暴露逻辑删除标记等持久化
 * 控制字段。日期字段输出到自然日，审计时间输出到秒。
 *
 * @author 角色C
 * @date 2026/07/09
 */
@Data
public class EquipmentLedgerRespVO {

    /** 设备台账主键。 */
    private Long id;

    /** 业务唯一的设备编码。 */
    private String equipmentCode;

    /** 面向生产、保养和报修人员展示的设备名称。 */
    private String equipmentName;

    /** 设备所属类别主键。 */
    private Long categoryId;

    /** 设备制造商主键，未维护时为空。 */
    private Long manufacturerId;

    /** 制造商定义的设备规格型号。 */
    private String equipmentModel;

    /** 用于追溯设备实物的出厂序列号。 */
    private String serialNumber;

    /** 设备当前所属车间主键。 */
    private Long workshopId;

    /** 设备当前所属产线主键。 */
    private Long productionLineId;

    /** 设备在车间或产线内的具体安装位置。 */
    private String installationLocation;

    /** 设备采购日期，按自然日序列化。 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate purchaseDate;

    /** 设备正式投产或启用日期，按自然日序列化。 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate commissioningDate;

    /** 设备业务运行状态：IDLE、RUNNING、STOPPED、REPAIRING、MAINTAINING 或 SCRAPPED。 */
    private String equipmentStatus;

    /** 设备现场负责人姓名或标识。 */
    private String responsiblePerson;

    /** 设备管理、使用限制等补充说明。 */
    private String remark;

    /** 主数据启停状态：1 表示启用，0 表示停用。 */
    private Integer status;

    /** 设备台账创建时间，由数据库维护。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 设备台账最后更新时间，由数据库维护。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
