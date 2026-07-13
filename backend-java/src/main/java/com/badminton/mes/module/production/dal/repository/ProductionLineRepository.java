package com.badminton.mes.module.production.dal.repository;

import java.util.List;
import java.util.Optional;

import com.badminton.mes.module.production.dal.entity.ProductionLineEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/**
 * 产线 JPA Repository，派工时校验产线可用性与产能，只读。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
public interface ProductionLineRepository extends JpaRepository<ProductionLineEntity, Long>,
        JpaSpecificationExecutor<ProductionLineEntity> {

    /**
     * 按主键查询未删除产线。
     *
     * @param id 产线主键
     * @return 产线实体
     */
    Optional<ProductionLineEntity> findByIdAndDeletedFalse(Long id);

    /**
     * 仅查询产线所属车间主键，按统一锁序加锁前使用。
     *
     * <p>标量查询不会把产线实体提前放入一级缓存，后续带写锁查询能够可靠取得
     * 最新实体状态。
     *
     * @param id 产线主键
     * @return 所属车间主键
     */
    @Query("""
            SELECT productionLine.workshopId FROM ProductionLineEntity productionLine
            WHERE productionLine.id = :id AND productionLine.deleted = false
            """)
    Optional<Long> findWorkshopIdById(@Param("id") Long id);

    /**
     * 按主键加悲观写锁查询未删除产线。
     *
     * @param id 产线主键
     * @return 产线实体
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT productionLine FROM ProductionLineEntity productionLine
            WHERE productionLine.id = :id AND productionLine.deleted = false
            """)
    Optional<ProductionLineEntity> findByIdAndDeletedFalseForUpdate(@Param("id") Long id);

    /**
     * 查询指定车间的启用产线，排产建议候选。
     *
     * @param workshopId 车间主键
     * @param status     状态(传启用)
     * @return 产线列表，无数据时为空集合
     */
    List<ProductionLineEntity> findByWorkshopIdAndStatusAndDeletedFalseOrderByIdAsc(
            Long workshopId, Integer status);

    /**
     * 判断有效产线编码是否存在。
     *
     * @param lineCode 产线编码
     * @return true 表示编码已存在
     */
    boolean existsByLineCodeAndDeletedFalse(String lineCode);

    /**
     * 修改时排除自身判断有效产线编码是否存在。
     *
     * @param lineCode 产线编码
     * @param id 排除的产线主键
     * @return true 表示编码已被其他产线占用
     */
    boolean existsByLineCodeAndIdNotAndDeletedFalse(String lineCode, Long id);

    /**
     * 判断车间下是否存在未删除产线。
     *
     * @param workshopId 车间主键
     * @return true 表示存在产线
     */
    boolean existsByWorkshopIdAndDeletedFalse(Long workshopId);

    /**
     * 判断车间下是否存在指定状态的未删除产线。
     *
     * @param workshopId 车间主键
     * @param status 产线状态
     * @return true 表示存在匹配产线
     */
    boolean existsByWorkshopIdAndStatusAndDeletedFalse(Long workshopId, Integer status);

    /**
     * 按产线编码查询未删除产线，外部任务单写入时解析编码用。
     *
     * @param lineCode 产线编码
     * @return 产线实体
     */
    Optional<ProductionLineEntity> findByLineCodeAndDeletedFalse(String lineCode);
}
