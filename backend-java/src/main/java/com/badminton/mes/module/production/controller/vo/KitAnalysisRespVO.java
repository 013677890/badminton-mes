package com.badminton.mes.module.production.controller.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 齐套分析结果响应 VO(逐物料行)。
 *
 * <p>物料编码/名称由服务端按物料档案回填，前端无需二次查询。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@Data
public class KitAnalysisRespVO {

    /** 主键 */
    private Long id;

    /** 生产工单 id */
    private Long workOrderId;

    /** 物料 id */
    private Long materialId;

    /** 物料编码 */
    private String materialCode;

    /** 物料名称 */
    private String materialName;

    /** 需求数量(剩余需求：需求-已领) */
    private BigDecimal requireQuantity;

    /** 可用数量(已扣锁定与在检) */
    private BigDecimal availableQuantity;

    /** 在途数量 */
    private BigDecimal transitQuantity;

    /** 欠料数量 */
    private BigDecimal shortageQuantity;

    /** 齐套状态：1 齐套 2 部分齐套 3 欠料 */
    private Integer kitStatus;

    /** 分析时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime analysisTime;
}
