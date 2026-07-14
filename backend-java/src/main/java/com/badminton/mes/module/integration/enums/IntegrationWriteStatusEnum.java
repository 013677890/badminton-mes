package com.badminton.mes.module.integration.enums;

import lombok.Getter;

/**
 * 外部接口写入状态。
 *
 * @author 张竹灏
 * @date 2026/07/11
 */
@Getter
public enum IntegrationWriteStatusEnum {

    /** 首次写入成功 */
    SUCCESS(1, "SUCCESS"),

    /** 业务校验或系统处理失败 */
    FAILED(2, "FAILED"),

    /** 幂等键已处理，未重复生成业务数据 */
    DUPLICATE(3, "DUPLICATE");

    /** 数据库存储值 */
    private final Integer status;

    /** 对外状态编码 */
    private final String code;

    IntegrationWriteStatusEnum(Integer status, String code) {
        this.status = status;
        this.code = code;
    }
}
