package com.badminton.mes.module.craft.constants;

import com.badminton.mes.common.core.ErrorCode;

/**
 * 工艺管理模块错误码。
 *
 * <p>复用黄山版错误码大类，具体业务语义放在 message 和 userTip 中。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
public final class CraftErrorCodeConstants {

    /** 工序不存在或已删除 */
    public static final ErrorCode PROCESS_NOT_EXISTS =
            new ErrorCode("A0402", "工序不存在", "工序不存在或已删除，请刷新后重试");

    /** 工序编码重复 */
    public static final ErrorCode PROCESS_CODE_DUPLICATE =
            new ErrorCode("A0506", "工序编码已存在", "工序编码重复，请更换后重试");

    /** 需要质检的工序未关联检验方案 */
    public static final ErrorCode PROCESS_QUALITY_PLAN_REQUIRED =
            new ErrorCode("A0420", "需要质检的工序必须关联检验方案", "请选择检验方案后重试");

    /** 检验方案不可用 */
    public static final ErrorCode PROCESS_QUALITY_PLAN_NOT_AVAILABLE =
            new ErrorCode("A0402", "工序关联的检验方案不可用", "请选择已启用的检验方案");

    /** 设备类别不可用 */
    public static final ErrorCode PROCESS_EQUIPMENT_CATEGORY_NOT_AVAILABLE =
            new ErrorCode("A0402", "工序关联的设备类别不可用", "请选择已启用的设备类别");

    /** 工序已被工艺路线引用 */
    public static final ErrorCode PROCESS_REFERENCED_BY_ROUTE =
            new ErrorCode("A0440", "工序已被工艺路线引用", "请先从工艺路线中移除该工序");

    /** 工序未启用，不可供新业务引用 */
    public static final ErrorCode PROCESS_NOT_ENABLED =
            new ErrorCode("A0440", "工序未启用", "请选择已启用的工序");

    /** 并发修改冲突 */
    public static final ErrorCode PROCESS_CONCURRENT_MODIFICATION =
            new ErrorCode("A0440", "工序已被其他用户修改", "工序数据已变更，请刷新后重试");

    /** 工序仍有关联子记录 */
    public static final ErrorCode PROCESS_HAS_ACTIVE_BINDINGS =
            new ErrorCode("A0440", "工序仍有关联的 SOP 或不良原因", "请先删除工序的 SOP 和不良原因");

    /** 工序 SOP 不存在 */
    public static final ErrorCode PROCESS_SOP_NOT_EXISTS =
            new ErrorCode("A0402", "工序 SOP 不存在", "SOP 不存在或已被删除，请刷新后重试");

    /** 同工序 SOP 编码重复 */
    public static final ErrorCode PROCESS_SOP_CODE_DUPLICATE =
            new ErrorCode("A0506", "同工序 SOP 编码已存在", "SOP 编码重复，请更换后重试");

    /** 工序 SOP 并发修改冲突 */
    public static final ErrorCode PROCESS_SOP_CONCURRENT_MODIFICATION =
            new ErrorCode("A0440", "工序 SOP 已被其他用户修改", "SOP 数据已变更，请刷新后重试");

    /** 工序不良原因不存在 */
    public static final ErrorCode PROCESS_DEFECT_REASON_NOT_EXISTS =
            new ErrorCode("A0402", "工序不良原因不存在", "不良原因不存在或已被删除，请刷新后重试");

    /** 同工序不良原因编码重复 */
    public static final ErrorCode PROCESS_DEFECT_REASON_CODE_DUPLICATE =
            new ErrorCode("A0506", "同工序不良原因编码已存在", "不良原因编码重复，请更换后重试");

    /** 工序不良原因并发修改冲突 */
    public static final ErrorCode PROCESS_DEFECT_REASON_CONCURRENT_MODIFICATION =
            new ErrorCode("A0440", "工序不良原因已被其他用户修改", "不良原因数据已变更，请刷新后重试");

    private CraftErrorCodeConstants() {
    }
}
