package com.badminton.mes.module.production.dal.repository;

import java.util.List;
import java.util.Optional;

import com.badminton.mes.module.production.dal.entity.KitShortageHandleEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 欠料处理记录 JPA Repository。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
public interface KitShortageHandleRepository extends JpaRepository<KitShortageHandleEntity, Long> {

    /**
     * 按主键查询未删除的处理记录。
     *
     * @param id 处理记录主键
     * @return 处理记录
     */
    Optional<KitShortageHandleEntity> findByIdAndDeletedFalse(Long id);

    /**
     * 查询指定工单的处理记录，最新在前。
     *
     * @param workOrderId 工单主键
     * @return 处理记录列表，无数据时为空集合
     */
    List<KitShortageHandleEntity> findByWorkOrderIdAndDeletedFalseOrderByIdDesc(Long workOrderId);

    /**
     * 查询指定物料最新一条未解决处理记录，看板取其预计到料日期。
     *
     * @param materialId   物料主键
     * @param handleStatus 处理状态(传处理中)
     * @return 最新处理记录
     */
    Optional<KitShortageHandleEntity> findFirstByMaterialIdAndHandleStatusAndDeletedFalseOrderByIdDesc(
            Long materialId, Integer handleStatus);

    /**
     * 处理状态流转 CAS：仅当当前状态等于前置状态才更新。
     *
     * @param id         处理记录主键
     * @param fromStatus 前置状态(处理中)
     * @param toStatus   目标状态(已解决)
     * @return 影响行数；1 成功，0 表示不存在、已删除或已被并发解决
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE KitShortageHandleEntity handle
            SET handle.handleStatus = :toStatus,
                handle.updateTime = CURRENT_TIMESTAMP
            WHERE handle.id = :id
              AND handle.handleStatus = :fromStatus
              AND handle.deleted = false
            """)
    int updateHandleStatus(@Param("id") Long id,
                           @Param("fromStatus") Integer fromStatus,
                           @Param("toStatus") Integer toStatus);
}
