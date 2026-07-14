package com.badminton.mes.module.device.controller.vo;

import com.badminton.mes.common.core.PageParam;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 设备接入配置分页请求。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DeviceAccessConfigPageReqVO extends PageParam {

    @Size(max = 64, message = "搜索关键字长度不能超过 64")
    private String keyword;

    @Positive(message = "设备台账必须为正整数")
    private Long equipmentId;

    @Positive(message = "关联工序必须为正整数")
    private Long processId;

    @Pattern(regexp = "^(NOT_TESTED|PASSED|FAILED)$", message = "联调状态不合法")
    private String commissioningStatus;

    @Min(value = 0, message = "正式采集状态只能为 0 或 1")
    @Max(value = 1, message = "正式采集状态只能为 0 或 1")
    private Integer enabledStatus;
}
