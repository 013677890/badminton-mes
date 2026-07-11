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

    /** 工艺路线不存在 */
    public static final ErrorCode ROUTE_NOT_EXISTS =
            new ErrorCode("A0402", "工艺路线不存在", "工艺路线不存在或已删除，请刷新后重试");

    /** 路线编码和业务版本重复 */
    public static final ErrorCode ROUTE_CODE_VERSION_DUPLICATE =
            new ErrorCode("A0506", "路线编码和版本已存在", "请更换路线编码或业务版本");

    /** 路线不是草稿 */
    public static final ErrorCode ROUTE_NOT_DRAFT =
            new ErrorCode("A0440", "只有草稿路线允许修改或删除", "请创建新版本后再修改");

    /** 路线不是生效状态 */
    public static final ErrorCode ROUTE_NOT_EFFECTIVE =
            new ErrorCode("A0440", "工艺路线未生效", "请选择已生效的工艺路线");

    /** 路线步骤顺序不连续 */
    public static final ErrorCode ROUTE_SEQUENCE_INVALID =
            new ErrorCode("A0420", "路线工序顺序必须从 1 开始且连续", "请重新调整工序顺序");

    /** 路线引用产品不可用 */
    public static final ErrorCode ROUTE_PRODUCT_NOT_AVAILABLE =
            new ErrorCode("A0402", "路线包含不可用产品", "请选择已启用的产品");

    /** 路线引用工序不可用 */
    public static final ErrorCode ROUTE_PROCESS_NOT_AVAILABLE =
            new ErrorCode("A0402", "路线包含不可用工序", "请选择已启用的工序");

    /** 路线引用工位不可用 */
    public static final ErrorCode ROUTE_STATION_NOT_AVAILABLE =
            new ErrorCode("A0402", "路线包含不可用工位", "请选择已启用的工位");

    /** 路线引用设备类别不可用 */
    public static final ErrorCode ROUTE_EQUIPMENT_CATEGORY_NOT_AVAILABLE =
            new ErrorCode("A0402", "路线包含不可用设备类别", "请选择已启用的设备类别");

    /** 路线引用 SOP 不可用 */
    public static final ErrorCode ROUTE_SOP_NOT_AVAILABLE =
            new ErrorCode("A0402", "路线包含不可用 SOP", "请选择所属工序下已启用的 SOP");

    /** 路线引用检验方案不可用 */
    public static final ErrorCode ROUTE_QUALITY_PLAN_NOT_AVAILABLE =
            new ErrorCode("A0402", "路线包含不可用检验方案", "请选择已启用的检验方案");

    /** 路线生效配置不完整 */
    public static final ErrorCode ROUTE_CONFIGURATION_INCOMPLETE =
            new ErrorCode("A0420", "路线工序控制配置不完整", "请补齐设备、质检标记、SOP 和检验方案");

    /** 路线并发修改冲突 */
    public static final ErrorCode ROUTE_CONCURRENT_MODIFICATION =
            new ErrorCode("A0440", "工艺路线已被其他用户修改", "路线数据已变更，请刷新后重试");

    /** 路线已被生产工单引用 */
    public static final ErrorCode ROUTE_REFERENCED_BY_WORK_ORDER =
            new ErrorCode("A0440", "工艺路线已被生产工单引用", "已使用路线只能停用或创建新版本");

    /** 产品未配置默认生效路线 */
    public static final ErrorCode ROUTE_DEFAULT_NOT_FOUND =
            new ErrorCode("A0402", "产品未配置默认生效路线", "请先审核并生效该产品的工艺路线");

    /** 派生版本草稿试图改变版本链身份 */
    public static final ErrorCode ROUTE_VERSION_IDENTITY_IMMUTABLE =
            new ErrorCode("A0440", "派生路线的编码和来源不允许修改", "请保持版本链路线编码与来源一致");

    /** 工序控制规则仍被生效路线引用 */
    public static final ErrorCode PROCESS_RULE_REFERENCED_BY_EFFECTIVE_ROUTE =
            new ErrorCode("A0440", "工序控制规则仍被生效路线引用", "请先创建并切换工艺路线新版本");

    /** SOP 仍被生效路线引用 */
    public static final ErrorCode PROCESS_SOP_REFERENCED_BY_EFFECTIVE_ROUTE =
            new ErrorCode("A0440", "SOP 仍被生效路线引用", "请先在工艺路线新版本中替换该 SOP");

    private CraftErrorCodeConstants() {
    }
}
