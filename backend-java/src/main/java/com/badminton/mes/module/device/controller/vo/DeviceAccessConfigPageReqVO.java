package com.badminton.mes.module.device.controller.vo;

import com.badminton.mes.common.core.PageParam;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 设备接入配置分页筛选条件。
 *
 * <p>支持按名称或编码关键字、设备、工序、联调状态和启用状态组合过滤；查询层会固定排除逻辑删除数据，
 * 关键字中的 SQL LIKE 通配符也会被转义，确保按用户输入字面含义搜索。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DeviceAccessConfigPageReqVO extends PageParam {

    /** 同时匹配配置编码、配置名称和采集点编码的模糊关键字。 */
    @Size(max = 64, message = "搜索关键字长度不能超过 64")
    private String keyword;

    /** 绑定设备台账主键的精确筛选条件。 */
    @Positive(message = "设备台账必须为正整数")
    private Long equipmentId;

    /** 关联工序主键的精确筛选条件。 */
    @Positive(message = "关联工序必须为正整数")
    private Long processId;

    /** 最近联调状态的精确筛选条件。 */
    @Pattern(regexp = "^(NOT_TESTED|PASSED|FAILED)$", message = "联调状态不合法")
    private String commissioningStatus;

    /** 正式采集启用状态的精确筛选条件。 */
    @Min(value = 0, message = "正式采集状态只能为 0 或 1")
    @Max(value = 1, message = "正式采集状态只能为 0 或 1")
    private Integer enabledStatus;
}
