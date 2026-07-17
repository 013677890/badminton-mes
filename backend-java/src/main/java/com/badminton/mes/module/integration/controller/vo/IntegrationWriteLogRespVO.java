package com.badminton.mes.module.integration.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 外部接口写入日志响应。
 *
 * <p>返回请求快照、处理状态、MES 结果定位和失败原因，既供运维查看，也支持根据外部业务键
 * 追踪幂等处理结果。逻辑删除和内部审计操作者等持久化控制字段不对外暴露。
 *
 * @author 张竹灏
 * @date 2026/07/11
 */
@Data
public class IntegrationWriteLogRespVO {

    /** 日志主键 */
    private Long id;

    /** 集成接口类型。 */
    private String interfaceType;

    /** 外部来源系统。 */
    private String sourceSystem;

    /** 来源侧业务键。 */
    private String businessKey;

    /** 外部请求 JSON 快照。 */
    private String requestSnapshot;

    /** 写入状态：成功、失败或重复。 */
    private Integer writeStatus;

    /** MES 业务主键 */
    private Long resultId;

    /** MES 业务编号 */
    private String resultNo;

    /** 失败错误码 */
    private String errorCode;

    /** 失败原因 */
    private String errorMessage;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
