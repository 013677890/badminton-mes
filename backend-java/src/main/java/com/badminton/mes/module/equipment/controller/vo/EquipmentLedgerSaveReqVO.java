package com.badminton.mes.module.equipment.controller.vo;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 设备台账创建/修改请求 VO。
 *
 * <p>基础字段规则使用注解声明，类别、制造商存在性与设备编码唯一性在 Service 层校验。
 *
 * @author 角色C
 * @date 2026/07/09
 */
@Data
public class EquipmentLedgerSaveReqVO {

    /** 设备编码，唯一 */
    @NotBlank(message = "设备编码不能为空")
    @Size(max = 32, message = "设备编码长度不能超过 32")
    @Pattern(regexp = "^(?!__DELETED_).+$", message = "设备编码不能使用系统保留前缀")
    private String equipmentCode;

    /** 设备名称 */
    @NotBlank(message = "设备名称不能为空")
    @Size(max = 128, message = "设备名称长度不能超过 128")
    private String equipmentName;

    /** 设备类别 id */
    @NotNull(message = "设备类别不能为空")
    @Positive(message = "设备类别必须为正整数")
    private Long categoryId;

    /** 设备制造商 id，可空 */
    @Positive(message = "设备制造商必须为正整数")
    private Long manufacturerId;

    /** 规格型号 */
    @Size(max = 64, message = "规格型号长度不能超过 64")
    private String equipmentModel;

    /** 出厂编号 */
    @Size(max = 64, message = "出厂编号长度不能超过 64")
    private String serialNumber;

    /** 所属车间 id，可空 */
    @Positive(message = "所属车间必须为正整数")
    private Long workshopId;

    /** 所属产线 id，可空 */
    @Positive(message = "所属产线必须为正整数")
    private Long productionLineId;

    /** 安装位置 */
    @Size(max = 128, message = "安装位置长度不能超过 128")
    private String installationLocation;

    /** 采购日期 */
    private LocalDate purchaseDate;

    /** 启用日期 */
    private LocalDate commissioningDate;

    /** 设备状态，可空，默认 IDLE */
    @Pattern(regexp = "^(IDLE|RUNNING|STOPPED|REPAIRING|MAINTAINING|SCRAPPED)$",
             message = "设备状态必须为 IDLE、RUNNING、STOPPED、REPAIRING、MAINTAINING、SCRAPPED 之一")
    private String equipmentStatus;

    /** 负责人 */
    @Size(max = 64, message = "负责人长度不能超过 64")
    private String responsiblePerson;

    /** 备注说明 */
    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;

    /** 状态：1 启用 0 停用，可空，默认 1 */
    @Min(value = 0, message = "状态取值为 0 或 1")
    @Max(value = 1, message = "状态取值为 0 或 1")
    private Integer status;
}
