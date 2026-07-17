package com.badminton.mes.module.barcode.controller.vo;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 条码作废请求 VO。
 *
 * <p>作废原因当前仅入日志留痕：契约基线 barcode 表未设原因列，
 * 如需持久化须先登记数据库变更。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Data
public class BarcodeCancelReqVO {

    /** 作废原因，可空 */
    @Size(max = 255, message = "作废原因长度不能超过 255")
    private String reason;
}
