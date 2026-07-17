package com.badminton.mes.module.integration.controller.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Data;

/**
 * 设备报工绑定保存请求。
 *
 * <p>请求只表达绑定主键和计数阈值等配置字段；产线、工序和默认员工是否存在且启用，以及自动
 * 报工开启时员工是否必填，由 Service 在事务内完成跨表校验。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Data
public class EquipmentBindingSaveReqVO {

    /** 被采集设备编码，保存时会统一去空格并转大写。 */
    @NotBlank(message = "设备编码不能为空")
    @Size(max = 32, message = "设备编码长度不能超过 32")
    private String equipmentCode;

    @NotNull(message = "产线不能为空")
    /** 设备所属产线主键，必须指向启用产线。 */
    private Long lineId;

    /** 可选固定工序主键，为空表示不限定工序。 */
    private Long processId;

    /** 自动报工默认员工主键，开启自动报工时不能为空。 */
    private Long defaultEmployeeId;

    /** 是否在有效计数后自动创建现场报工。 */
    @NotNull(message = "是否自动报工不能为空")
    private Boolean autoReport;

    @NotNull(message = "最大计数增量不能为空")
    @Positive(message = "最大计数增量必须大于 0")
    /** 单次累计增量上限，超过时计数进入异常池。 */
    private Long maxIncrement;

    /** 绑定启停状态：1 启用，0 停用。 */
    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态只能为 0 或 1")
    @Max(value = 1, message = "状态只能为 0 或 1")
    private Integer status;
}
