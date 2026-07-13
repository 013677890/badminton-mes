package com.badminton.mes.module.scene.controller.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 待确认报工审核请求。
 *
 * @author Codex
 * @date 2026/07/13
 */
@Data
public class WorkReportApproveReqVO {

    @NotNull(message = "员工不能为空")
    private Long employeeId;
}
