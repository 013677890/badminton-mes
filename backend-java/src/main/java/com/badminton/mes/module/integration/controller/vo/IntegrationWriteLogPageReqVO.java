package com.badminton.mes.module.integration.controller.vo;

import com.badminton.mes.common.core.PageParam;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 外部接口写入日志分页查询参数。
 *
 * <p>条件均为可选精确筛选，接口类型、来源系统和状态分别对应日志的业务分区、调用来源和
 * 处理结果；分页查询只读审计数据，不会改变幂等结果。
 *
 * @author 张竹灏
 * @date 2026/07/11
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class IntegrationWriteLogPageReqVO extends PageParam {

    /** 集成接口类型，用于隔离不同写入链路。 */
    @Pattern(regexp = "^(UNIT_WRITE|WORK_ORDER_WRITE|DISPATCH_ORDER_WRITE|ERP_TASK_SYNC|"
            + "ERP_CRAFT_SYNC|DEVICE_COUNT_WRITE|COMPLETION_ORDER_READ)$",
            message = "接口类型不合法")
    private String interfaceType;

    /** 外部来源系统，按规范化编码筛选。 */
    @Size(max = 32, message = "来源系统长度不能超过 32")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "来源系统包含不支持的字符")
    private String sourceSystem;

    /** 来源侧业务键，用于定位一次具体外部请求。 */
    @Size(max = 64, message = "业务键长度不能超过 64")
    private String businessKey;

    /** 写入状态：1 成功、2 失败、3 重复。 */
    @Min(value = 1, message = "写入状态最小值为 1")
    @Max(value = 3, message = "写入状态最大值为 3")
    private Integer writeStatus;
}
