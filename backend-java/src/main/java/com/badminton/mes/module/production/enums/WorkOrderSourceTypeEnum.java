package com.badminton.mes.module.production.enums;

import lombok.Getter;

/**
 * 生产工单来源枚举，对应 prod_work_order.source_type 字段。
 *
 * @author 张竹灏
 * @date 2026/07/07
 */
@Getter
public enum WorkOrderSourceTypeEnum {

    /** 手工录入：PMC 计划员在管理端直接创建 */
    MANUAL(1, "手工录入"),

    /** Excel 导入 */
    IMPORT(2, "导入"),

    /** ERP 同步：由接口模块从 ERP 读取生成 */
    ERP_SYNC(3, "ERP同步"),

    /** API 写入：外部系统调用开放接口写入 */
    API_WRITE(4, "API写入");

    /** 来源值，与数据库 source_type 字段取值一致 */
    private final Integer type;

    /** 来源描述 */
    private final String description;

    WorkOrderSourceTypeEnum(Integer type, String description) {
        this.type = type;
        this.description = description;
    }
}
