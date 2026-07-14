package com.badminton.mes.module.equipment.controller.vo;

import com.badminton.mes.common.core.PageParam;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 设备台账分页查询请求 VO。
 *
 * <p>支持设备编码/名称/型号模糊匹配，也支持类别、制造商、设备状态、启停状态等精确筛选。
 *
 * @author 角色C
 * @date 2026/07/09
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EquipmentLedgerPageReqVO extends PageParam {

    /** 设备编码、名称或型号，模糊匹配，可空 */
    @Size(max = 64, message = "搜索关键字长度不能超过 64")
    private String keyword;

    /** 设备类别 id，可空 */
    @Positive(message = "设备类别必须为正整数")
    private Long categoryId;

    /** 设备制造商 id，可空 */
    @Positive(message = "设备制造商必须为正整数")
    private Long manufacturerId;

    /** 设备状态，可空 */
    @Pattern(regexp = "^(IDLE|RUNNING|STOPPED|REPAIRING|MAINTAINING|SCRAPPED)$",
             message = "设备状态必须为 IDLE、RUNNING、STOPPED、REPAIRING、MAINTAINING、SCRAPPED 之一")
    private String equipmentStatus;

    /** 所属车间 id，可空 */
    @Positive(message = "所属车间必须为正整数")
    private Long workshopId;

    /** 所属产线 id，可空 */
    @Positive(message = "所属产线必须为正整数")
    private Long productionLineId;

    /** 状态筛选，可空，取值 0 或 1 */
    @Min(value = 0, message = "状态取值为 0 或 1")
    @Max(value = 1, message = "状态取值为 0 或 1")
    private Integer status;
}
