package com.badminton.mes.module.production.dal.dataobject;

import lombok.Data;

/**
 * 车间数据对象，对应表 base_workshop。
 *
 * <p>基础资料模块尚未建设，本类仅映射生产订单模块创建工单时
 * 存在性校验需要读取的列(ORM-001)。
 *
 * @author 张竹灏
 * @date 2026/07/07
 */
@Data
public class WorkshopDO {

    /** 主键 */
    private Long id;

    /** 车间编码(唯一) */
    private String workshopCode;

    /** 车间名称 */
    private String workshopName;

    /** 状态：1 启用 0 停用 */
    private Integer status;
}
