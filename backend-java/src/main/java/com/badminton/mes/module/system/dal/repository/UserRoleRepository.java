package com.badminton.mes.module.system.dal.repository;

import java.util.Collection;
import java.util.List;

import com.badminton.mes.module.system.dal.entity.UserRoleEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 用户角色关系 JPA Repository。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
public interface UserRoleRepository extends JpaRepository<UserRoleEntity, Long> {

    /**
     * 查询用户全部角色关系(含已逻辑删除)。
     *
     * <p>uk_user_role 唯一键覆盖已删除行，重新授予历史角色须"复活"旧行
     * 而不能新插入，因此调整角色前需要读全量关系。
     *
     * @param userId 用户主键
     * @return 角色关系列表(含已删除)，无数据时为空集合
     */
    List<UserRoleEntity> findByUserId(Long userId);

    /**
     * 查询用户当前有效的角色关系。
     *
     * @param userId 用户主键
     * @return 角色关系列表，无数据时为空集合
     */
    List<UserRoleEntity> findByUserIdAndDeletedFalse(Long userId);

    /**
     * 批量查询多个用户的有效角色关系，分页回填角色信息时避免逐行查询。
     *
     * @param userIds 用户主键集合
     * @return 角色关系列表，无数据时为空集合
     */
    List<UserRoleEntity> findByUserIdInAndDeletedFalse(Collection<Long> userIds);

    /**
     * 按角色反查有效的用户关系(安灯按角色匹配处理人等场景)。
     *
     * @param roleId 角色主键
     * @return 角色关系列表，无数据时为空集合
     */
    List<UserRoleEntity> findByRoleIdAndDeletedFalse(Long roleId);

    /**
     * 逻辑删除用户的全部角色关系，调整角色与删除用户时使用。
     *
     * @param userId 用户主键
     * @return 影响行数
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE UserRoleEntity userRole
            SET userRole.deleted = true,
                userRole.updateTime = CURRENT_TIMESTAMP
            WHERE userRole.userId = :userId
              AND userRole.deleted = false
            """)
    int logicDeleteByUserId(@Param("userId") Long userId);
}
