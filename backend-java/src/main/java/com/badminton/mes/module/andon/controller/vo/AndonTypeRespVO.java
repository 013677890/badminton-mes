package com.badminton.mes.module.andon.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 安灯类型详情响应。
 *
 * <p>类型是事件处理策略的主数据，返回异常分类、状态流转模式、默认响应与指派规则、通知渠道和灯控开关，
 * 供事件发起界面及类型维护界面展示。
 */
@Data
public class AndonTypeRespVO {

    /** 类型数据库主键，被原因、处理配置和事件引用。 */
    private Long id;
    /** 类型业务编码，用于稳定识别和系统间传递。 */
    private String typeCode;
    /** 类型显示名称，供现场人员选择和查看。 */
    private String typeName;
    /** 异常类别：生产、设备、质量、物料或非生产异常。 */
    private String exceptionCategory;
    /** 处理方式：无需处理、自行处理或请求协助，决定事件初始状态与指派逻辑。 */
    private String handlingMode;
    /** 类型级默认响应时限，单位为分钟；协助模式无匹配配置时用于生成响应期限。 */
    private Integer responseMinutes;
    /** 类型级默认责任角色编码，协助模式无匹配配置时作为初始角色指派。 */
    private String responsibleRoleCode;
    /** 类型级默认通知渠道，使用逗号分隔的应用内、短信或微信渠道。 */
    private String notificationChannels;
    /** 灯控开关；为真时事件发起会尝试开启设备安灯，闭环时再关闭。 */
    private Boolean lightControlEnabled;
    /** 启用状态：{@code 1} 可用于新事件，{@code 0} 停用但保留历史引用。 */
    private Integer enabledStatus;
    /** 类型用途、处理边界或维护约定的补充说明。 */
    private String remark;

    /** 类型记录创建时间，按 {@code yyyy-MM-dd HH:mm:ss} 输出。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 类型规则、灯控开关或启用状态最后修改时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
