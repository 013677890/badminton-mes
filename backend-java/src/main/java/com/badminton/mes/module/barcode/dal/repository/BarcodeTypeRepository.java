package com.badminton.mes.module.barcode.dal.repository;

import java.util.List;
import java.util.Optional;

import com.badminton.mes.module.barcode.dal.entity.BarcodeTypeEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 条码类型 JPA Repository。
 *
 * <p>状态流转、信息修改和逻辑删除使用返回影响行数的 JPQL 更新，
 * 保留数据库层 CAS 语义，防止并发窗口内覆盖他人变更。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
public interface BarcodeTypeRepository extends JpaRepository<BarcodeTypeEntity, Long>,
        JpaSpecificationExecutor<BarcodeTypeEntity> {

    /**
     * 按主键查询未删除的条码类型。
     *
     * @param id 类型主键
     * @return 类型实体
     */
    Optional<BarcodeTypeEntity> findByIdAndDeletedFalse(Long id);

    /**
     * 判断未删除类型中是否已存在指定编码。
     *
     * @param typeCode 类型编码
     * @return true 存在，false 不存在
     */
    boolean existsByTypeCodeAndDeletedFalse(String typeCode);

    /**
     * 判断除指定主键外的未删除类型中是否已存在指定编码，修改时排除自身查重。
     *
     * @param typeCode 类型编码
     * @param id       排除的类型主键
     * @return true 存在，false 不存在
     */
    boolean existsByTypeCodeAndIdNotAndDeletedFalse(String typeCode, Long id);

    /**
     * 按状态查询未删除类型，编码升序，用于启用类型选项。
     *
     * @param status 状态值
     * @return 类型列表
     */
    List<BarcodeTypeEntity> findByStatusAndDeletedFalseOrderByTypeCodeAsc(Integer status);

    /**
     * 修改类型基础信息，deleted = false 条件构成 CAS，防止并发删除后误复活。
     *
     * @param id          类型主键
     * @param typeCode    类型编码
     * @param typeName    类型名称
     * @param applyObject 适用对象说明，可空
     * @return 影响行数；0 表示类型不存在或已删除
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE BarcodeTypeEntity barcodeType
            SET barcodeType.typeCode = :typeCode,
                barcodeType.typeName = :typeName,
                barcodeType.applyObject = :applyObject,
                barcodeType.updateTime = CURRENT_TIMESTAMP
            WHERE barcodeType.id = :id
              AND barcodeType.deleted = false
            """)
    int updateInfo(@Param("id") Long id,
                   @Param("typeCode") String typeCode,
                   @Param("typeName") String typeName,
                   @Param("applyObject") String applyObject);

    /**
     * 状态流转 CAS：仅当当前状态等于前置状态才更新，用于启用/停用。
     *
     * @param id         类型主键
     * @param fromStatus 前置状态
     * @param toStatus   目标状态
     * @return 影响行数；0 表示类型不存在、已删除或状态不满足
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE BarcodeTypeEntity barcodeType
            SET barcodeType.status = :toStatus,
                barcodeType.updateTime = CURRENT_TIMESTAMP
            WHERE barcodeType.id = :id
              AND barcodeType.status = :fromStatus
              AND barcodeType.deleted = false
            """)
    int updateStatus(@Param("id") Long id,
                     @Param("fromStatus") Integer fromStatus,
                     @Param("toStatus") Integer toStatus);

    /**
     * 逻辑删除，deleted = false 条件构成 CAS，防止重复删除。
     *
     * @param id 类型主键
     * @return 影响行数；0 表示类型不存在或已删除
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE BarcodeTypeEntity barcodeType
            SET barcodeType.deleted = true,
                barcodeType.updateTime = CURRENT_TIMESTAMP
            WHERE barcodeType.id = :id
              AND barcodeType.deleted = false
            """)
    int logicDeleteById(@Param("id") Long id);
}
