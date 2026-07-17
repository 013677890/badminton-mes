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
 * <p>一个请求属于单一来源系统，包含不超过 500 条物料快照。服务层会检查批内物料编码重复，
 * 并只接受同步时间更新的快照；reanalyze 为 true 时才触发受影响工单齐套重算。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Data
public class MaterialStockBatchReqVO {

    /** 库存快照来源系统，参与库存分区和同步时间判断。 */
    @NotBlank(message = "来源系统不能为空")
    @Size(max = 32, message = "来源系统长度不能超过 32")
    private String sourceSystem;

    /** 是否在接受新库存后重新分析受影响的生产工单。 */
    private Boolean reanalyze;

    /** 本次批量库存快照明细，启用级联校验并限制批量大小。 */
    @Valid
    @NotEmpty(message = "库存快照不能为空")
    @Size(max = 500, message = "单次最多同步 500 条库存快照")
    private List<MaterialStockItemReqVO> items;
}
