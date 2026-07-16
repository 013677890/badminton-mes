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
 * <p>注解负责必填、长度、正数、枚举和启停范围等单字段约束。设备编码唯一性，类别、制造商、
 * 车间和产线的存在性及归属关系，以及采购日期与启用日期的业务合理性由 Service 统一校验。
 *
 * @author 角色C
 * @date 2026/07/09
 */
@Data
public class EquipmentLedgerSaveReqVO {

    /** 业务唯一的设备编码；禁止使用逻辑删除记录占用的系统保留前缀。 */
    @NotBlank(message = "设备编码不能为空")
    @Size(max = 32, message = "设备编码长度不能超过 32")
    @Pattern(regexp = "^(?!__DELETED_).+$", message = "设备编码不能使用系统保留前缀")
    private String equipmentCode;

    /** 面向生产、保养和报修人员展示的设备名称。 */
    @NotBlank(message = "设备名称不能为空")
    @Size(max = 128, message = "设备名称长度不能超过 128")
    private String equipmentName;

    /** 设备类别主键，必须指向可用于建档的有效类别。 */
    @NotNull(message = "设备类别不能为空")
    @Positive(message = "设备类别必须为正整数")
    private Long categoryId;

    /** 设备制造商主键，可空；非空时必须指向有效制造商。 */
    @Positive(message = "设备制造商必须为正整数")
    private Long manufacturerId;

    /** 制造商定义的规格型号，可空，最大 64 个字符。 */
    @Size(max = 64, message = "规格型号长度不能超过 64")
    private String equipmentModel;

    /** 设备出厂序列号，可空，用于追溯具体实物。 */
    @Size(max = 64, message = "出厂编号长度不能超过 64")
    private String serialNumber;

    /** 当前所属车间主键，可空；非空时必须指向有效车间。 */
    @Positive(message = "所属车间必须为正整数")
    private Long workshopId;

    /** 当前所属产线主键，可空；同时填写车间时须满足组织归属关系。 */
    @Positive(message = "所属产线必须为正整数")
    private Long productionLineId;

    /** 设备在车间或产线内的具体安装位置，可空。 */
    @Size(max = 128, message = "安装位置长度不能超过 128")
    private String installationLocation;

    /** 设备采购日期，可空，仅表示自然日。 */
    private LocalDate purchaseDate;

    /** 设备正式投产或启用日期，可空；日期关系由 Service 校验。 */
    private LocalDate commissioningDate;

    /** 业务运行状态；可空并默认 IDLE，枚举覆盖闲置、运行、停机、维修、保养和报废。 */
    @Pattern(regexp = "^(IDLE|RUNNING|STOPPED|REPAIRING|MAINTAINING|SCRAPPED)$",
             message = "设备状态必须为 IDLE、RUNNING、STOPPED、REPAIRING、MAINTAINING、SCRAPPED 之一")
    private String equipmentStatus;

    /** 设备现场负责人姓名或标识，可空，最大 64 个字符。 */
    @Size(max = 64, message = "负责人长度不能超过 64")
    private String responsiblePerson;

    /** 设备管理、使用限制等补充说明，可空，最大 255 个字符。 */
    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;

    /** 主数据启停状态：1 表示启用，0 表示停用；可空并默认 1。 */
    @Min(value = 0, message = "状态取值为 0 或 1")
    @Max(value = 1, message = "状态取值为 0 或 1")
    private Integer status;
}
