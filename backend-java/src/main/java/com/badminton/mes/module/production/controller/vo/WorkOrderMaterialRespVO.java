package com.badminton.mes.module.production.controller.vo;

import java.math.BigDecimal;

import lombok.Data;

/**
 * 工单物料需求响应 VO。
 *
 * <p>物料编码/名称由服务端按物料档案回填，前端无需二次查询。
 *
 * @author 张竹灏
 * @date 2026/07/08
 */
@Data
public class WorkOrderMaterialRespVO {

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

    /** 需求数量(计划数×BOM用量) */
    private BigDecimal requireQuantity;

    /** 已领/已发数量 */
    private BigDecimal issuedQuantity;
}
