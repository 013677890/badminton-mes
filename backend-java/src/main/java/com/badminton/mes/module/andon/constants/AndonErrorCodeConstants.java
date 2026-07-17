package com.badminton.mes.module.andon.constants;

import com.badminton.mes.common.core.ErrorCode;

/**
 * 安灯管理模块错误码定义。
 *
 * <p>错误码按“类型、处理配置、原因、事件”四组业务对象组织，供 Controller 统一转换为前端可识别的失败响应。
 * 这里同时区分数据不存在、唯一性冲突、规则不完整、引用保护和操作权限等失败语义，便于调用方决定是刷新数据、
 * 修正输入还是切换有权限的操作人。常量只描述已经由服务层判定的业务事实，不在此处承载任何流程判断。
 */
public final class AndonErrorCodeConstants {

    /** 类型不存在或已被逻辑删除，通常用于详情读取和加锁更新的前置校验。 */
    public static final ErrorCode TYPE_NOT_EXISTS =
            new ErrorCode("A0402", "安灯类型不存在", "安灯类型不存在或已删除，请刷新后重试");
    /** 类型编码违反活动数据唯一性；数据库唯一约束异常也会归一化为该错误。 */
    public static final ErrorCode TYPE_CODE_DUPLICATE =
            new ErrorCode("A0506", "安灯类型编码已存在", "请更换安灯类型编码后重试");
    /** 类型的处理模式与响应时限、责任角色或通知渠道组合不满足业务约束。 */
    public static final ErrorCode TYPE_RULE_INVALID =
            new ErrorCode("A0420", "安灯类型处理规则不合法", "协助处理类型必须配置响应时限和责任角色");
    /** 类型仍被配置、原因或历史事件引用，引用链存在时禁止删除主数据。 */
    public static final ErrorCode TYPE_HAS_REFERENCES =
            new ErrorCode("A0440", "安灯类型已被使用", "存在异常配置、原因或异常记录的类型不能删除");
    /** 安灯处理配置不存在或已逻辑删除。 */
    public static final ErrorCode CONFIGURATION_NOT_EXISTS =
            new ErrorCode("A0402", "异常配置不存在", "异常配置不存在或已删除，请刷新后重试");
    /** 同一类型在同一产线范围重复配置；全局范围也作为一个独立范围参与唯一性判断。 */
    public static final ErrorCode CONFIGURATION_SCOPE_DUPLICATE =
            new ErrorCode("A0506", "异常配置范围已存在", "同一安灯类型和产线范围只能配置一条处理规则");
    /** 处理主体、响应时限与升级主体/时限之间的成组规则不完整或前后矛盾。 */
    public static final ErrorCode CONFIGURATION_RULE_INVALID =
            new ErrorCode("A0420", "异常配置规则不合法", "请配置有效处理人或处理角色，并检查响应与升级时限");
    /** 类型仍有未关闭事件时保护处理规则快照，避免在途事件的指派和时限依据发生漂移。 */
    public static final ErrorCode CONFIGURATION_HAS_ACTIVE_EVENTS =
            new ErrorCode("A0440", "异常配置仍被活动事件使用", "请先关闭该类型的活动异常，再删除处理配置");
    /** 配置引用的用户已停用，或角色不在当前启用角色集合中。 */
    public static final ErrorCode RESPONSIBLE_SUBJECT_INVALID =
            new ErrorCode("A0420", "异常责任主体不可用", "请选择启用的处理用户或有效角色");
    /** 原因不存在或已被逻辑删除。 */
    public static final ErrorCode REASON_NOT_EXISTS =
            new ErrorCode("A0402", "异常原因不存在", "异常原因不存在或已删除，请刷新后重试");
    /** 原因编码与其他活动原因冲突，或逻辑删除时生成的保留编码发生冲突。 */
    public static final ErrorCode REASON_CODE_DUPLICATE =
            new ErrorCode("A0506", "异常原因编码已存在", "请更换异常原因编码后重试");
    /** 原因已经作为申报原因或实际原因进入事件历史，禁止删除或跨类型迁移。 */
    public static final ErrorCode REASON_HAS_EVENTS =
            new ErrorCode("A0440", "异常原因已被使用", "已被异常记录使用的原因不能删除，请停用该原因");
    /** 事件不存在或已逻辑删除。 */
    public static final ErrorCode EVENT_NOT_EXISTS =
            new ErrorCode("A0402", "安灯异常不存在", "安灯异常不存在或已删除，请刷新后重试");
    /** 高并发发起事件时生成的事件单号发生唯一性碰撞。 */
    public static final ErrorCode EVENT_NO_DUPLICATE =
            new ErrorCode("A0506", "安灯异常单号重复", "异常单号生成冲突，请稍后重试");
    /** 当前事件状态不在动作允许的源状态集合中，防止越级或重复执行状态迁移。 */
    public static final ErrorCode EVENT_STATUS_INVALID =
            new ErrorCode("A0420", "当前状态不允许执行该操作", "请刷新异常状态后重试");
    /** 工单、设备、质检记录或原因与事件上下文不一致。 */
    public static final ErrorCode EVENT_REFERENCE_INVALID =
            new ErrorCode("A0420", "异常关联数据不合法", "请检查原因、工单、设备和质量检验记录是否匹配");
    /** 当前依赖模块尚无可靠公共查询接口，服务端选择拒绝不可验证的关联而非静默接受。 */
    public static final ErrorCode EVENT_REFERENCE_UNSUPPORTED =
            new ErrorCode("A0420", "异常关联数据暂不支持校验", "生产任务和工序尚未提供公共查询能力，暂不能直接关联");
    /** 协助处理事件未匹配到产线级配置或全局兜底配置。 */
    public static final ErrorCode EVENT_CONFIGURATION_NOT_MATCHED =
            new ErrorCode("A0420", "未匹配到异常处理配置", "请先为安灯类型和产线配置启用的处理规则");
    /** 转派或升级目标既不是可用用户，也不是有效角色。 */
    public static final ErrorCode EVENT_ASSIGNEE_INVALID =
            new ErrorCode("A0420", "异常处理对象不可用", "请选择启用的处理用户或有效角色");
    /** 当前登录人不属于事件指派用户、指派角色或管理角色。 */
    public static final ErrorCode EVENT_OPERATION_FORBIDDEN =
            new ErrorCode("A0403", "无权处理当前异常", "仅当前处理人、处理角色或管理人员可以执行该操作");
    /** 完成或转派动作缺少流程审计所需的原因、处理结果、影响数据或操作说明。 */
    public static final ErrorCode EVENT_RESULT_INCOMPLETE =
            new ErrorCode("A0420", "异常处理结果不完整", "请填写实际原因、处理结果和有效影响数据");

    private AndonErrorCodeConstants() {
    }
}
