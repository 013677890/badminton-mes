package com.badminton.mes.module.system.service;

import java.util.List;

import com.badminton.mes.module.system.controller.vo.RoleRespVO;
import com.badminton.mes.module.system.controller.vo.RoleUserRespVO;

/**
 * 系统角色 Service 接口。角色为种子数据固化，只提供查询。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
public interface RoleService {

    /**
     * 查询启用角色列表，用户表单下拉使用。
     *
     * @return 启用角色列表，按 id 升序，无数据时为空集合(API-002)
     */
    List<RoleRespVO> getEnabledRoles();

    /**
     * 查询允许在小程序中自助注册的职位。
     *
     * @return 可注册职位列表，按 id 升序
     */
    List<RoleRespVO> getRegistrationRoles();

    /**
     * 判断角色是否允许在小程序中自助注册。
     *
     * @param roleId 角色主键
     * @return true 表示允许自助注册
     */
    boolean isRegistrationRole(Long roleId);

    /**
     * 按角色反查启用用户(安灯按角色匹配处理人等场景)。
     *
     * @param roleId 角色主键
     * @return 拥有该角色的启用用户列表，无数据时为空集合(API-002)
     * @throws com.badminton.mes.common.exception.ServiceException 角色不存在时抛出
     */
    List<RoleUserRespVO> getRoleUsers(Long roleId);
}
