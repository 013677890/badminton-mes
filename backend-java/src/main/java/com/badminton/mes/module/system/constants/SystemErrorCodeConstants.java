package com.badminton.mes.module.system.constants;

import com.badminton.mes.common.core.ErrorCode;

/**
 * 系统模块(认证、用户、角色)错误码，与业务代码就近维护。
 *
 * <p>编码复用《Java开发手册(黄山版)》附表 3 宏观错误码(ERRCODE-006)，
 * 业务细节由 message 承载(ERRCODE-008)。跨模块通用的 A0230/A0301
 * 放在 {@code GlobalErrorCodeConstants}。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
public final class SystemErrorCodeConstants {

    /**
     * 工号或密码错误(A0200 用户登录异常)。
     * 账户不存在与密码错误统一提示，防止撞库探测账号；服务端日志区分具体原因。
     */
    public static final ErrorCode LOGIN_CREDENTIALS_INVALID =
            new ErrorCode("A0200", "工号或密码错误", "工号或密码错误，请重新输入");

    /** 密码连续错误次数超限，登录临时锁定 */
    public static final ErrorCode LOGIN_LOCKED =
            new ErrorCode("A0211", "密码错误次数超限", "连续失败次数过多，请 15 分钟后重试");

    /** 账户已停用，密码校验通过后才提示，避免向未持凭据者泄露账号状态 */
    public static final ErrorCode LOGIN_USER_DISABLED =
            new ErrorCode("A0202", "账户已停用", "账号已被停用，请联系管理员");

    /** 用户不存在或已删除(用户管理接口) */
    public static final ErrorCode USER_NOT_EXISTS =
            new ErrorCode("A0201", "用户不存在", "用户不存在或已被删除，请刷新后重试");

    /** 工号重复(应用层先查校验，数据库唯一索引 uk_user_no 兜底) */
    public static final ErrorCode USER_NO_DUPLICATE =
            new ErrorCode("A0506", "工号已存在", "工号重复，请更换工号后重试");

    /** 修改本人密码时旧密码校验失败 */
    public static final ErrorCode USER_OLD_PASSWORD_MISMATCH =
            new ErrorCode("A0210", "旧密码错误", "旧密码不正确，请重新输入");

    /** 新增用户缺少初始密码(创建与修改共用 VO，密码仅创建时必填) */
    public static final ErrorCode USER_INIT_PASSWORD_REQUIRED =
            new ErrorCode("A0402", "新增用户必须填写初始密码", "请填写初始密码");

    /** 分配的角色不存在、已删除或已停用 */
    public static final ErrorCode USER_ROLE_INVALID =
            new ErrorCode("A0402", "所选角色不存在或已停用", "所选角色不可用，请重新选择");

    /** 用户所属车间或产线不存在、未启用或层级不一致 */
    public static final ErrorCode USER_ORGANIZATION_INVALID =
            new ErrorCode("A0402", "用户所属车间或产线不可用", "请选择层级一致的启用车间和产线");

    /** 角色不存在或已删除(按角色反查用户等接口) */
    public static final ErrorCode ROLE_NOT_EXISTS =
            new ErrorCode("A0402", "角色不存在", "角色不存在或已被删除，请刷新后重试");

    /** 不能停用或删除当前登录账号，防止管理员误操作后无人能恢复 */
    public static final ErrorCode USER_OPERATE_SELF_FORBIDDEN =
            new ErrorCode("A0440", "不能停用或删除当前登录账号", "不能对当前登录账号执行该操作");

    private SystemErrorCodeConstants() {
    }
}
