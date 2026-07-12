package com.badminton.mes.module.production.enums;

import java.util.Arrays;
import java.util.List;

import lombok.Getter;

/**
 * 生产工单状态枚举，对应 prod_work_order.order_status 字段。
 *
 * <p>状态机流转：已创建 → 已下达 → 生产中 ⇄ 暂停 → 已完工 → 已关闭；
 * 已创建状态可作废。样例仅实现"已创建 → 已下达"的下达动作，
 * 其余流转由现场管理模块驱动。
 *
 * @author 张竹灏
 * @date 2026/07/07
 */
@Getter
public enum WorkOrderStatusEnum {

    /** 已创建：允许修改、删除、下达 */
    CREATED(0, "已创建"),

    /** 已下达：等待排产派工，不允许直接修改基础信息 */
    RELEASED(1, "已下达"),

    /** 生产中：已有任务开工 */
    IN_PRODUCTION(2, "生产中"),

    /** 暂停：生产过程临时挂起 */
    PAUSED(3, "暂停"),

    /** 已完工：全部任务完成 */
    FINISHED(4, "已完工"),

    /** 已关闭：业务终态，不能再生成生产任务 */
    CLOSED(5, "已关闭"),

    /** 已作废：创建错误等场景的业务作废 */
    CANCELLED(6, "已作废");

    /** 状态值，与数据库 order_status 字段取值一致 */
    private final Integer status;

    /** 状态描述，用于展示与日志(避免与枚举内置 name() 混淆，不命名为 name) */
    private final String description;

    /** 非终态工单状态，用于主数据活动引用校验。 */
    private static final List<Integer> ACTIVE_STATUSES = Arrays.stream(values())
            .filter(status -> status != CLOSED && status != CANCELLED)
            .map(WorkOrderStatusEnum::getStatus)
            .toList();

    WorkOrderStatusEnum(Integer status, String description) {
        this.status = status;
        this.description = description;
    }

    /**
     * 返回全部非终态状态值。
     *
     * <p>集合按枚举定义顺序稳定返回且不可修改；新增中间状态会自动纳入。
     *
     * @return 非已关闭、非已作废状态值
     */
    public static List<Integer> activeStatuses() {
        return ACTIVE_STATUSES;
    }
}
