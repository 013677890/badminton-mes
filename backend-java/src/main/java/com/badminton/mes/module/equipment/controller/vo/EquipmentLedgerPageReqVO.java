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
 * <p>所有业务条件均可空，并可与 {@link PageParam} 的页码和每页数量组合使用。关键字匹配设备编码、
 * 名称或型号，其余条件采用精确匹配；查询层统一追加逻辑删除过滤。
 *
 * @author 角色C
 * @date 2026/07/09
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EquipmentLedgerPageReqVO extends PageParam {

    /** 设备编码、名称或型号关键字，采用模糊匹配，可空，最大 64 个字符。 */
    @Size(max = 64, message = "搜索关键字长度不能超过 64")
    private String keyword;

    /** 设备类别主键，精确筛选该类别下的设备；可空且非空时必须为正数。 */
    @Positive(message = "设备类别必须为正整数")
    private Long categoryId;

    /** 设备制造商主键，精确筛选该制造商的设备；可空且非空时必须为正数。 */
    @Positive(message = "设备制造商必须为正整数")
    private Long manufacturerId;

    /** 设备业务状态，可按闲置、运行、停机、维修、保养或报废精确筛选。 */
    @Pattern(regexp = "^(IDLE|RUNNING|STOPPED|REPAIRING|MAINTAINING|SCRAPPED)$",
             message = "设备状态必须为 IDLE、RUNNING、STOPPED、REPAIRING、MAINTAINING、SCRAPPED 之一")
    private String equipmentStatus;

    /** 所属车间主键，精确筛选车间内设备；可空且非空时必须为正数。 */
    @Positive(message = "所属车间必须为正整数")
    private Long workshopId;

    /** 所属产线主键，精确筛选产线内设备；可空且非空时必须为正数。 */
    @Positive(message = "所属产线必须为正整数")
    private Long productionLineId;

    /** 主数据启停状态：1 表示启用，0 表示停用；为空时查询全部状态。 */
    @Min(value = 0, message = "状态取值为 0 或 1")
    @Max(value = 1, message = "状态取值为 0 或 1")
    private Integer status;
}
