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
