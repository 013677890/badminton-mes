package com.badminton.mes.module.scene.controller.vo;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 报工提交请求。
 *
 * @author 刘涵
 * @date 2026/07/13
 */
@Data
public class SceneWorkReportSubmitReqVO {

    /** 客户端幂等请求号 */
    @NotBlank
    @Size(max = 64)
    private String requestNo;

    /** 工序派工明细 id */
    @NotNull
    @Positive
    private Long dispatchDetailId;

    /** 投入数量 */
    @PositiveOrZero
    private int inputQuantity;

    /** 良品数量 */
    @PositiveOrZero
    private int goodQuantity;

    /** 不良数量 */
    @PositiveOrZero
    private int defectQuantity;

    /** 返修数量 */
    @PositiveOrZero
    private int reworkQuantity;

    /** 报工扫码值；是否必填由 must_scan_report 生效参数决定 */
    @Size(max = 64)
    private String barcodeValue;

    /** 业务报工时间 */
    @NotNull
    private LocalDateTime reportTime;
}
