package com.badminton.mes.module.production.service;

/**
 * 生产组织引用查询契约，供系统用户模块校验车间与产线归属。
 *
 * @author 张竹灏
 * @date 2026/07/12
 */
public interface ProductionOrganizationReferenceQuery {

    /**
     * 写锁校验用户所属车间与产线均启用且层级一致。
     *
     * <p>车间与产线都为空时表示后台管理员不绑定生产范围，返回 true；
     * 产线非空而车间为空时返回 false。
     *
     * @param workshopId 车间主键，可空
     * @param lineId 产线主键，可空
     * @return true 表示归属合法
     */
    boolean lockAndCheckAssignment(Long workshopId, Long lineId);
}
