package com.badminton.mes.module.system.dal.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.badminton.mes.module.system.dal.entity.UserEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * 系统用户 JPA Repository。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
public interface UserRepository extends JpaRepository<UserEntity, Long>, JpaSpecificationExecutor<UserEntity> {

    /**
     * 按主键查询未删除用户。
     *
     * @param id 用户主键
     * @return 未删除的用户
     */
    Optional<UserEntity> findByIdAndDeletedFalse(Long id);

    /**
     * 按主键集合批量查询未删除用户，按角色反查用户等场景使用。
     *
     * @param ids 用户主键集合
     * @return 命中的用户列表
     */
    List<UserEntity> findByIdInAndDeletedFalse(Collection<Long> ids);

    /**
     * 按工号查询未删除用户，登录与工号查重使用。
     *
     * @param userNo 工号
     * @return 未删除的用户
     */
    Optional<UserEntity> findByUserNoAndDeletedFalse(String userNo);

    /**
     * 判断工号是否已被未删除用户占用。
     *
     * @param userNo 工号
     * @return true 已占用
     */
    boolean existsByUserNoAndDeletedFalse(String userNo);
}
