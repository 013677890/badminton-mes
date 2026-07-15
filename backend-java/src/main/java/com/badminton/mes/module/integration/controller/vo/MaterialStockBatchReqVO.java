package com.badminton.mes.module.integration.controller.vo;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * WMS/ERP 物料库存批量同步请求。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Data
public class MaterialStockBatchReqVO {

    @NotBlank(message = "来源系统不能为空")
    @Size(max = 32, message = "来源系统长度不能超过 32")
    private String sourceSystem;

    private Boolean reanalyze;

    @Valid
    @NotEmpty(message = "库存快照不能为空")
    @Size(max = 500, message = "单次最多同步 500 条库存快照")
    private List<MaterialStockItemReqVO> items;
}
