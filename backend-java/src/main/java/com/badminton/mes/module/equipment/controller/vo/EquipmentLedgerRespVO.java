package com.badminton.mes.module.equipment.controller.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 设备台账响应 VO，设备详情与分页列表共用。
 *
 * <p>只暴露业务展示字段，逻辑删除标记等内部字段不出参。
 *
 * @author 角色C
 * @date 2026/07/09
 */
@Data
public class EquipmentLedgerRespVO {

    /** 设备主键 */
    private Long id;

    /** 设备编码 */
    private String equipmentCode;

    /** 设备名称 */
    private String equipmentName;

    /** 设备类别 id */
    private Long categoryId;

    /** 设备制造商 id */
    private Long manufacturerId;

    /** 规格型号 */
    private String equipmentModel;

    /** 出厂编号 */
    private String serialNumber;

    /** 所属车间 id */
    private Long workshopId;

    /** 所属产线 id */
    private Long productionLineId;

    /** 安装位置 */
    private String installationLocation;

    /** 采购日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate purchaseDate;

    /** 启用日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate commissioningDate;

    /** 设备状态 */
    private String equipmentStatus;

    /** 负责人 */
    private String responsiblePerson;

    /** 备注说明 */
    private String remark;

    /** 状态：1 启用 0 停用 */
    private Integer status;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
