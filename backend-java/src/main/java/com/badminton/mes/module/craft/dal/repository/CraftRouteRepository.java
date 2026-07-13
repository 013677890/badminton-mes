package com.badminton.mes.module.craft.dal.repository;

import java.util.Optional;

import com.badminton.mes.module.craft.dal.entity.CraftRouteEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/**
 * 工艺路线主档 Repository。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
public interface CraftRouteRepository extends JpaRepository<CraftRouteEntity, Long>,
        JpaSpecificationExecutor<CraftRouteEntity> {

    /**
     * 查询未删除路线。
     *
     * @param id 路线主键
     * @return 路线实体
     */
    Optional<CraftRouteEntity> findByIdAndDeletedFalse(Long id);

    /**
     * 以写锁查询未删除路线。
     *
     * @param id 路线主键
     * @return 路线实体
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT route FROM CraftRouteEntity route WHERE route.id = :id AND route.deleted = false")
    Optional<CraftRouteEntity> findByIdAndDeletedFalseForUpdate(@Param("id") Long id);

    /**
     * 判断路线编码和业务版本是否重复。
     *
     * @param routingCode    路线编码
     * @param routingVersion 业务版本
     * @return true 表示重复
     */
    boolean existsByRoutingCodeAndRoutingVersionAndDeletedFalse(
            String routingCode, String routingVersion);

    /**
     * 判断路线编码和业务版本是否重复，排除指定路线。
     *
     * @param routingCode    路线编码
     * @param routingVersion 业务版本
     * @param id             排除路线主键
     * @return true 表示重复
     */
    boolean existsByRoutingCodeAndRoutingVersionAndIdNotAndDeletedFalse(
            String routingCode, String routingVersion, Long id);
}
