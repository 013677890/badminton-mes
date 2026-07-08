package com.badminton.mes.module.production.dal.dataobject;

import lombok.Data;

/**
 * 产品数据对象，对应表 base_product。
 *
 * <p>基础资料模块尚未建设，本类仅映射生产订单模块创建工单时
 * 需要读取的列(存在性校验 + 冗余字段回填)，不查询无关列(ORM-001)。
 *
 * @author 张竹灏
 * @date 2026/07/07
 */
@Data
public class ProductDO {

    /** 主键 */
    private Long id;

    /** 产品编码(唯一) */
    private String productCode;

    /** 产品名称 */
    private String productName;

    /** 规格型号 */
    private String spec;

    /** 计量单位 id */
    private Long unitId;

    /** 状态：1 启用 0 停用 */
    private Integer status;
}
