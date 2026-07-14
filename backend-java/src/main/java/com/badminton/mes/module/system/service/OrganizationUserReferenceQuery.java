package com.badminton.mes.module.system.service;

import java.util.Collection;
import java.util.Map;

/**
 * 车间与产线的系统用户反向引用查询契约。
 *
 * @author 张竹灏
 * @date 2026/07/12
 */
public interface OrganizationUserReferenceQuery {

    /**
     * 判断用户是否存在、未删除且启用。
     *
     * @param userId 用户主键
     * @return true 表示用户可用
     */
    boolean isEnabledUser(Long userId);

    /**
     * 判断车间是否被任意未删除用户引用。
     *
     * @param workshopId 车间主键
     * @return true 表示存在用户引用
     */
    boolean hasAnyWorkshopUser(Long workshopId);

    /**
     * 判断车间是否被启用用户引用。
     *
     * @param workshopId 车间主键
     * @return true 表示存在启用用户引用
     */
    boolean hasEnabledWorkshopUser(Long workshopId);

    /**
     * 判断产线是否被任意未删除用户引用。
     *
     * @param lineId 产线主键
     * @return true 表示存在用户引用
     */
    boolean hasAnyProductionLineUser(Long lineId);

    /**
     * 判断产线是否被启用用户引用。
     *
     * @param lineId 产线主键
     * @return true 表示存在启用用户引用
     */
    boolean hasEnabledProductionLineUser(Long lineId);

    /**
     * 批量加载未删除用户姓名，车间分页回填主管姓名使用。
     *
     * @param userIds 用户主键集合
     * @return 用户主键 -> 姓名映射，不存在的用户不在映射中
     */
    Map<Long, String> loadUserNames(Collection<Long> userIds);
}
