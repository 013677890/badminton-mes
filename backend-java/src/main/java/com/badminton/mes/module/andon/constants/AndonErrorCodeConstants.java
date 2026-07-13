package com.badminton.mes.module.andon.constants;

import com.badminton.mes.common.core.ErrorCode;

/** 安灯管理模块错误码。 */
public final class AndonErrorCodeConstants {

    public static final ErrorCode TYPE_NOT_EXISTS =
            new ErrorCode("A0402", "安灯类型不存在", "安灯类型不存在或已删除，请刷新后重试");
    public static final ErrorCode TYPE_CODE_DUPLICATE =
            new ErrorCode("A0506", "安灯类型编码已存在", "请更换安灯类型编码后重试");
    public static final ErrorCode TYPE_RULE_INVALID =
            new ErrorCode("A0420", "安灯类型处理规则不合法", "协助处理类型必须配置响应时限和责任角色");
    public static final ErrorCode TYPE_HAS_REFERENCES =
            new ErrorCode("A0440", "安灯类型已被使用", "存在异常配置、原因或异常记录的类型不能删除");
    public static final ErrorCode CONFIGURATION_NOT_EXISTS =
            new ErrorCode("A0402", "异常配置不存在", "异常配置不存在或已删除，请刷新后重试");
    public static final ErrorCode CONFIGURATION_SCOPE_DUPLICATE =
            new ErrorCode("A0506", "异常配置范围已存在", "同一安灯类型和产线范围只能配置一条处理规则");
    public static final ErrorCode CONFIGURATION_RULE_INVALID =
            new ErrorCode("A0420", "异常配置规则不合法", "请配置有效处理人或处理角色，并检查响应与升级时限");
    public static final ErrorCode CONFIGURATION_HAS_ACTIVE_EVENTS =
            new ErrorCode("A0440", "异常配置仍被活动事件使用", "请先关闭该类型的活动异常，再删除处理配置");
    public static final ErrorCode RESPONSIBLE_SUBJECT_INVALID =
            new ErrorCode("A0420", "异常责任主体不可用", "请选择启用的处理用户或有效角色");
    public static final ErrorCode REASON_NOT_EXISTS =
            new ErrorCode("A0402", "异常原因不存在", "异常原因不存在或已删除，请刷新后重试");
    public static final ErrorCode REASON_CODE_DUPLICATE =
            new ErrorCode("A0506", "异常原因编码已存在", "请更换异常原因编码后重试");
    public static final ErrorCode REASON_HAS_EVENTS =
            new ErrorCode("A0440", "异常原因已被使用", "已被异常记录使用的原因不能删除，请停用该原因");
    public static final ErrorCode EVENT_NOT_EXISTS =
            new ErrorCode("A0402", "安灯异常不存在", "安灯异常不存在或已删除，请刷新后重试");
    public static final ErrorCode EVENT_NO_DUPLICATE =
            new ErrorCode("A0506", "安灯异常单号重复", "异常单号生成冲突，请稍后重试");
    public static final ErrorCode EVENT_STATUS_INVALID =
            new ErrorCode("A0420", "当前状态不允许执行该操作", "请刷新异常状态后重试");
    public static final ErrorCode EVENT_REFERENCE_INVALID =
            new ErrorCode("A0420", "异常关联数据不合法", "请检查原因、工单、设备和质量检验记录是否匹配");
    public static final ErrorCode EVENT_REFERENCE_UNSUPPORTED =
            new ErrorCode("A0420", "异常关联数据暂不支持校验", "生产任务和工序尚未提供公共查询能力，暂不能直接关联");
    public static final ErrorCode EVENT_CONFIGURATION_NOT_MATCHED =
            new ErrorCode("A0420", "未匹配到异常处理配置", "请先为安灯类型和产线配置启用的处理规则");
    public static final ErrorCode EVENT_ASSIGNEE_INVALID =
            new ErrorCode("A0420", "异常处理对象不可用", "请选择启用的处理用户或有效角色");
    public static final ErrorCode EVENT_OPERATION_FORBIDDEN =
            new ErrorCode("A0403", "无权处理当前异常", "仅当前处理人、处理角色或管理人员可以执行该操作");
    public static final ErrorCode EVENT_RESULT_INCOMPLETE =
            new ErrorCode("A0420", "异常处理结果不完整", "请填写实际原因、处理结果和有效影响数据");

    private AndonErrorCodeConstants() {
    }
}
