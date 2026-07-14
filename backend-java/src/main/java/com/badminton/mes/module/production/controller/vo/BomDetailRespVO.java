package com.badminton.mes.module.production.controller.vo;

import java.math.BigDecimal;

import lombok.Data;

/** BOM 明细响应。 */
@Data
public class BomDetailRespVO {
    /** 明细主键 */
    private Long id;
    /** 物料主键 */
    private Long materialId;
    /** 物料编码 */
    private String materialCode;
    /** 物料名称 */
    private String materialName;
    /** 标准用量 */
    private BigDecimal quantity;
    /** 损耗率百分比 */
    private BigDecimal lossRate;
}
