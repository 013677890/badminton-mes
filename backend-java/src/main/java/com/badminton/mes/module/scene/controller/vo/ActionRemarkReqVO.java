package com.badminton.mes.module.scene.controller.vo;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 审核、作废等动作的备注请求。
 *
 * @author Codex
 * @date 2026/07/13
 */
@Data
public class ActionRemarkReqVO {

    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;
}
