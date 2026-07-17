package com.badminton.mes.module.integration.dal.repository;

import java.util.Optional;

import com.badminton.mes.module.integration.dal.entity.UnitEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/**
 * 计量单位 Repository。
 *
 * <p>计量单位是产品和外部工单的基础主数据。有效读取统一排除逻辑删除；编码写锁用于外部 upsert，
 * 主键写锁用于产品等调用方在同一事务中确认单位启用状态。
 *
 * @author 张竹灏
 * @date 2026/07/11
 */
public interface UnitRepository extends JpaRepository<UnitEntity, Long> {

    /**
     * 按主键查询未删除单位。
     *
     * @param id 单位主键
     * @return 计量单位
     */
    Optional<UnitEntity> findByIdAndDeletedFalse(Long id);

    /**
     * 按主键悲观写锁查询未删除单位，供基础资料写入校验。
     *
     * <p>锁持续到当前事务结束，保证调用方后续依赖的启用状态不会在校验后立即改变。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT unit FROM UnitEntity unit WHERE unit.id = :id AND unit.deleted = false")
    Optional<UnitEntity> findByIdAndDeletedFalseForUpdate(@Param("id") Long id);

    /**
     * 按编码查询有效单位。
     *
     * @param unitCode 单位编码
     * @return 计量单位
     */
    Optional<UnitEntity> findByUnitCodeAndDeletedFalse(String unitCode);

    /**
     * 按编码写锁查询有效单位，串行化同一编码的外部 upsert。
     *
     * @param unitCode 单位编码
     * @return 计量单位
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT unit FROM UnitEntity unit "
            + "WHERE unit.unitCode = :unitCode AND unit.deleted = false")
    Optional<UnitEntity> findByUnitCodeForUpdate(@Param("unitCode") String unitCode);
}
