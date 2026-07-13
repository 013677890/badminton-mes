package com.badminton.mes.module.production.controller.vo;

import lombok.Data;

/** 车间摘要响应，供跨模块校验车间引用使用。 */
@Data
public class WorkshopRespVO {

    private Long id;

    private String workshopCode;

    private String workshopName;

    private Integer status;
}
