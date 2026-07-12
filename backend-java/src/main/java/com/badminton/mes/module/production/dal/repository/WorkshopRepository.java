package com.badminton.mes.module.production.dal.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.badminton.mes.module.production.dal.entity.WorkshopEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/**
 * 车间 JPA Repository。
 *
 * @author 张竹灏
 * @date 2026/07/08
 */
public interface WorkshopRepository extends JpaRepository<WorkshopEntity, Long>,
        JpaSpecificationExecutor<WorkshopEntity> {

    /**
     * 按主键查询未删除的车间。
     *
     * @param id 车间主键
     * @return 车间实体
     */
    Optional<WorkshopEntity> findByIdAndDeletedFalse(Long id);

    /**
     * 按主键加悲观写锁查询未删除车间。
     *
     * @param id 车间主键
     * @return 车间实体
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT workshop FROM WorkshopEntity workshop
            WHERE workshop.id = :id AND workshop.deleted = false
            """)
    Optional<WorkshopEntity> findByIdAndDeletedFalseForUpdate(@Param("id") Long id);

    /**
     * 按车间编码查询未删除车间。
     *
     * @param workshopCode 车间编码
     * @return 车间实体
     */
    Optional<WorkshopEntity> findByWorkshopCodeAndDeletedFalse(String workshopCode);

    /**
     * 按编码加写锁查询未删除车间，外部工单写入使用。
     *
     * @param workshopCode 车间编码
     * @return 车间实体
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT workshop FROM WorkshopEntity workshop
            WHERE workshop.workshopCode = :workshopCode AND workshop.deleted = false
            """)
    Optional<WorkshopEntity> findByWorkshopCodeAndDeletedFalseForUpdate(
            @Param("workshopCode") String workshopCode);

    /**
     * 判断有效车间编码是否存在。
     *
     * @param workshopCode 车间编码
     * @return true 表示编码已存在
     */
    boolean existsByWorkshopCodeAndDeletedFalse(String workshopCode);

    /**
     * 修改时排除自身判断有效车间编码是否存在。
     *
     * @param workshopCode 车间编码
     * @param id 排除的车间主键
     * @return true 表示编码已被其他车间占用
     */
    boolean existsByWorkshopCodeAndIdNotAndDeletedFalse(String workshopCode, Long id);

    /**
     * 批量查询未删除车间，产线分页回填车间信息使用。
     *
     * @param ids 车间主键集合
     * @return 车间列表
     */
    List<WorkshopEntity> findByIdInAndDeletedFalse(Collection<Long> ids);
}
