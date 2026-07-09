package com.badminton.mes.module.system.dal.repository;

import java.util.Collection;
import java.util.List;

import com.badminton.mes.module.system.dal.entity.RoleEntity;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 系统角色 JPA Repository。角色为种子数据，只读。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
public interface RoleRepository extends JpaRepository<RoleEntity, Long> {

    /**
     * 查询指定状态的全部未删除角色，按 id 升序。
     *
     * @param status 状态(1 启用 0 停用)
     * @return 角色列表，无数据时为空集合
     */
    List<RoleEntity> findByStatusAndDeletedFalseOrderByIdAsc(Integer status);

    /**
     * 按主键集合查询未删除角色，用户分配角色时校验角色可用。
     *
     * @param ids 角色主键集合
     * @return 命中的角色列表
     */
    List<RoleEntity> findByIdInAndDeletedFalse(Collection<Long> ids);

    /**
     * 按主键集合查询未删除角色并按 id 升序，回填用户角色信息使用。
     *
     * @param ids 角色主键集合
     * @return 命中的角色列表，按 id 升序
     */
    List<RoleEntity> findByIdInAndDeletedFalseOrderByIdAsc(Collection<Long> ids);
}
