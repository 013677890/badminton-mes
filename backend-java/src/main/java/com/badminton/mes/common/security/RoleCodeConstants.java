package com.badminton.mes.common.security;

/**
 * 内置角色编码常量，取值与 system 模块 V3、craft 模块 V6 迁移种子数据一致。
 *
 * <p>供各模块 {@code @RequiresRoles} 注解与业务代码引用，避免魔法字符串
 * 散落；属于跨模块共享的安全词汇表，故放在 common(协作边界约定：
 * 公共能力沉淀到 common)。角色为固定档案，不提供增删改。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
public final class RoleCodeConstants {

    /** 管理员：系统管理、用户与角色维护 */
    public static final String ADMIN = "ADMIN";

    /** PMC 计划员：生产计划与工单管理 */
    public static final String PMC = "PMC";

    /** 车间主管：车间生产执行管理 */
    public static final String WORKSHOP_MANAGER = "WORKSHOP_MANAGER";

    /** 班组长：班组派工与现场作业 */
    public static final String TEAM_LEADER = "TEAM_LEADER";

    /** 操作工：工位报工与作业执行 */
    public static final String OPERATOR = "OPERATOR";

    /** 质检员：检验与质量记录 */
    public static final String INSPECTOR = "INSPECTOR";

    /** 工艺工程师：工序、工艺路线与 SOP 维护 */
    public static final String CRAFT_ENGINEER = "CRAFT_ENGINEER";

    private RoleCodeConstants() {
    }
}
