package com.badminton.mes.module.production.controller.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 欠料看板汇总响应 VO(按物料聚合)。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@Data
public class ShortageBoardRespVO {

    /** 物料 id */
    private Long materialId;

    /** 物料编码 */
    private String materialCode;

    /** 物料名称 */
    private String materialName;

    /** 欠料总量(各工单欠料求和) */
    private BigDecimal totalShortage;

    /** 影响工单数 */
    private Long affectedOrderCount;

    /** 在途数量 */
    private BigDecimal transitQuantity;

    /** 预计到料日期(该物料最新一条未解决处理记录，未登记时为空) */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expectedArrivalDate;
}
