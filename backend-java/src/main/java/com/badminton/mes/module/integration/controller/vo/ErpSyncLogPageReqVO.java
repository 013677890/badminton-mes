package com.badminton.mes.module.integration.controller.vo;

import com.badminton.mes.common.core.PageParam;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * ERP 同步日志分页查询参数（固定查询 interfaceType=ERP_TASK_SYNC）。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ErpSyncLogPageReqVO extends PageParam {

    /** ERP 来源系统，按规范化编码精确筛选。 */
    @Size(max = 32, message = "来源系统长度不能超过 32")
    private String sourceSystem;

    /** ERP 任务单号，对应日志 businessKey。 */
    @Size(max = 64, message = "业务键长度不能超过 64")
    private String businessKey;

    /** 同步状态：1 成功、2 失败、3 重复。 */
    @Min(value = 1, message = "同步状态最小值为 1")
    @Max(value = 3, message = "同步状态最大值为 3")
    private Integer writeStatus;
}
