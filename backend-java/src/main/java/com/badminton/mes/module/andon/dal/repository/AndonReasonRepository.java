package com.badminton.mes.module.andon.dal.repository;

import java.util.List;
import java.util.Optional;

import com.badminton.mes.module.andon.dal.entity.AndonReasonEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/**
 * 安灯异常原因数据访问接口。
 *
 * <p>支持原因编码唯一性检查、并发维护所需的悲观写锁，以及类型变更和原因删除保护所需的引用查询。
 */
public interface AndonReasonRepository extends JpaRepository<AndonReasonEntity, Long>,
        JpaSpecificationExecutor<AndonReasonEntity> {

    /** 按主键读取未逻辑删除的原因，供详情展示等只读场景使用。 */
    Optional<AndonReasonEntity> findByIdAndDeletedFalse(Long id);

    /** 以悲观写锁读取活动原因，防止并发更新、停用或删除覆盖彼此结果。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select reason from AndonReasonEntity reason where reason.id = :id and reason.deleted = false")
    Optional<AndonReasonEntity> findByIdAndDeletedFalseForUpdate(@Param("id") Long id);

    /** 检查原因编码是否被未删除原因占用，用于新增时的活动数据唯一性校验。 */
    boolean existsByReasonCodeAndDeletedFalse(String reasonCode);

    /** 更新原因时排除自身，检查编码是否与其他未删除原因冲突。 */
    boolean existsByReasonCodeAndIdNotAndDeletedFalse(String reasonCode, Long id);

    /** 检查编码是否曾在任何原因记录中出现，包含逻辑删除数据。 */
    boolean existsByReasonCode(String reasonCode);

    /**
     * 仅查询指定安灯类型下未删除原因的主键，供类型变更后级联失效原因详情缓存。
     */
    @Query("select reason.id from AndonReasonEntity reason "
            + "where reason.andonTypeId = :andonTypeId and reason.deleted = false")
    List<Long> findIdsByAndonTypeIdAndDeletedFalse(@Param("andonTypeId") Long andonTypeId);

    /**
     * 统计原因作为事件初步原因或实际原因的引用总数。
     *
     * <p>任一引用存在都说明原因已经进入事件历史，服务层据此阻止删除或改变其归属类型。
     */
    @Query(value = "select count(*) from andon_event where is_deleted = false "
            + "and (reason_id = :reasonId or actual_reason_id = :reasonId)", nativeQuery = true)
    long countEventReferences(@Param("reasonId") Long reasonId);
}
