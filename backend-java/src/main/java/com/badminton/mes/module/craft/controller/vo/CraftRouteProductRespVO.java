package com.badminton.mes.module.craft.controller.vo;

import lombok.Data;

/**
 * 工艺路线适用产品响应 VO。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
@Data
public class CraftRouteProductRespVO {

    /** 产品主键 */
    private Long productId;

    /** 产品编码 */
    private String productCode;

    /** 产品名称 */
    private String productName;

    /** 是否产品当前默认路线 */
    private Boolean defaultRoute;
}
