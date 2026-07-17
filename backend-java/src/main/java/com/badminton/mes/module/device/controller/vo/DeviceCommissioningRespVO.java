package com.badminton.mes.module.device.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 设备联调记录详情响应。
 *
 * <p>同时返回通信、数据格式和综合结论，供管理端还原配置启用资格的形成过程；问题说明与样例报文
 * 用于失败联调的排障和协议复现，不代表当前配置仍处于同一状态。
 */
@Data
public class DeviceCommissioningRespVO {

    /** 联调记录主键。 */
    private Long id;
    /** 被测试的接入配置主键。 */
    private Long accessConfigId;

    /** 实际执行联调的业务时间，由请求方提供且登记时不得晚于服务器当前时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime testTime;

    /** 执行联调的用户主键。 */
    private Long testerUserId;
    /** 通信链路检查结果：{@code SUCCESS} 成功或 {@code FAILED} 失败。 */
    private String communicationResult;
    /** 报文字段与结构检查结果：{@code SUCCESS} 成功或 {@code FAILED} 失败。 */
    private String dataFormatResult;
    /** 本次联调综合结论：{@code PASSED} 通过或 {@code FAILED} 失败。 */
    private String testResult;
    /** 失败原因及排障说明；任一检查失败时必有内容。 */
    private String issueDescription;
    /** 联调过程中留存的设备样例报文。 */
    private String samplePayload;

    /** 服务端持久化联调历史记录的时间，不等同于实际联调业务时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
