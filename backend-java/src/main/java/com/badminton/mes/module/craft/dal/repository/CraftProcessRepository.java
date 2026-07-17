package com.badminton.mes.module.craft.dal.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.badminton.mes.module.craft.dal.entity.CraftProcessEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/**
 * 工序档案 JPA Repository。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
public interface CraftProcessRepository extends JpaRepository<CraftProcessEntity, Long>,
        JpaSpecificationExecutor<CraftProcessEntity> {

    /**
     * 按主键查询未删除工序。
     *
     * @param id 工序主键
     * @return 工序实体
     */
    Optional<CraftProcessEntity> findByIdAndDeletedFalse(Long id);

    /**
     * 按主键和状态查询未删除工序，供只接受指定状态的跨模块场景使用。
     *
     * @param id 工序主键
     * @param status 目标状态
     * @return 同时满足状态与软删除条件的工序
     */
    Optional<CraftProcessEntity> findByIdAndStatusAndDeletedFalse(Long id, Integer status);

    /**
     * 以写锁查询未删除工序，和路线审核的引用锁串行化。
     *
     * @param id 工序主键
     * @return 工序实体
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT process FROM CraftProcessEntity process "
            + "WHERE process.id = :id AND process.deleted = false")
    Optional<CraftProcessEntity> findByIdAndDeletedFalseForUpdate(@Param("id") Long id);

    /**
     * 判断未删除工序中是否存在指定编码。
     *
     * @param processCode 工序编码
     * @return true 存在，false 不存在
     */
    boolean existsByProcessCodeAndDeletedFalse(String processCode);

    /**
     * 按编码查询未删除工序，供 ERP 工艺同步通过编码查找工序主键。
     *
     * @param processCode 工序编码
     * @return 工序实体
     */
    Optional<CraftProcessEntity> findByProcessCodeAndDeletedFalse(String processCode);

    /**
     * 以写锁按编码查询未删除工序，和设备计数校验期间的工序变更串行化。
     *
     * @param processCode 工序编码
     * @return 已锁定工序实体
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT process FROM CraftProcessEntity process "
            + "WHERE process.processCode = :processCode AND process.deleted = false")
    Optional<CraftProcessEntity> findByProcessCodeAndDeletedFalseForUpdate(
            @Param("processCode") String processCode);

    /**
     * 判断未删除工序中是否存在指定编码，排除当前工序。
     *
     * @param processCode 工序编码
     * @param id          排除的工序主键
     * @return true 存在，false 不存在
     */
    boolean existsByProcessCodeAndIdNotAndDeletedFalse(String processCode, Long id);

    /**
     * 批量查询已启用工序规则，供工艺路线、现场和计件模块使用。
     *
     * @param ids    工序主键集合
     * @param status 启用状态
     * @return 工序列表
     */
    List<CraftProcessEntity> findByIdInAndStatusAndDeletedFalse(Collection<Long> ids, Integer status);

    /**
     * 按主键升序写锁可用工序，供路线审核在锁内复核引用。
     *
     * @param ids    工序主键集合
     * @param status 启用状态
     * @return 已锁定工序列表
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT process FROM CraftProcessEntity process
            WHERE process.id IN :ids
              AND process.status = :status
              AND process.deleted = false
            ORDER BY process.id ASC
            """)
    List<CraftProcessEntity> findAvailableByIdInForUpdateOrderByIdAsc(
            @Param("ids") Collection<Long> ids, @Param("status") Integer status);

    /**
     * 判断设备类别是否仍被启用工序引用。
     *
     * @param equipmentCategoryId 设备类别主键
     * @param status              工序启用状态
     * @return true 表示仍有启用工序引用
     */
    boolean existsByEquipmentCategoryIdAndStatusAndDeletedFalse(
            Long equipmentCategoryId, Integer status);
}
