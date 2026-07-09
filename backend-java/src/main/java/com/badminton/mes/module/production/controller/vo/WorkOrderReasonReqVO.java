package com.badminton.mes.module.production.controller.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 工单暂停/作废原因请求 VO。
 *
 * <p>暂停与作废属于异常流转，原因必填以满足留痕要求。
 *
 * @author 张竹灏
 * @date 2026/07/08
 */
@Data
public class WorkOrderReasonReqVO {

    /** 操作原因 */
    @NotBlank(message = "操作原因不能为空")
    @Size(max = 255, message = "操作原因长度不能超过 255")
    private String reason;
}
