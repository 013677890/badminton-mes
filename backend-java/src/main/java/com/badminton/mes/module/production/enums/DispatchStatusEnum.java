package com.badminton.mes.module.production.enums;

import java.util.Arrays;
import java.util.List;

import lombok.Getter;

/**
 * 派工单状态枚举，对应 dispatch_order.dispatch_status 字段。
 *
 * <p>状态机：待审核 →(审核)→ 已审核 →(下发)→ 已下发；待审核/已审核/已下发
 * 可取消(回退工单已派数量)。执行中/已完成由现场管理模块回写，本模块只保留状态值。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@Getter
public enum DispatchStatusEnum {

    /** 待审核：新建派工单初始状态，可修改可取消 */
    PENDING_AUDIT(0, "待审核"),

    /** 已审核：审核通过等待下发 */
    AUDITED(1, "已审核"),

    /** 已下发：已推送到现场执行，调整须记录原因 */
    ISSUED(2, "已下发"),

    /** 执行中：现场已开工(现场模块回写) */
    EXECUTING(3, "执行中"),

    /** 已完成：现场执行完毕(现场模块回写) */
    FINISHED(4, "已完成"),

    /** 已取消：业务作废，工单已派数量已回退 */
    CANCELLED(5, "已取消");

    /** 状态值，与数据库 dispatch_status 字段取值一致 */
    private final Integer status;

    /** 状态描述 */
    private final String description;

    /** 未结束的派工状态，用于产线停用前引用校验。 */
    private static final List<Integer> ACTIVE_STATUSES = Arrays.stream(values())
            .filter(status -> status != FINISHED && status != CANCELLED)
            .map(DispatchStatusEnum::getStatus)
            .toList();

    DispatchStatusEnum(Integer status, String description) {
        this.status = status;
        this.description = description;
    }

    /**
     * 返回全部未结束派工状态。
     *
     * @return 非已完成、非已取消状态值
     */
    public static List<Integer> activeStatuses() {
        return ACTIVE_STATUSES;
    }
}
