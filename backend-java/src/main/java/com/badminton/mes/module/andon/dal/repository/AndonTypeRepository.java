package com.badminton.mes.module.andon.dal.repository;

import java.util.Optional;

import com.badminton.mes.module.andon.dal.entity.AndonTypeEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/**
 * 安灯类型数据访问接口。
 *
 * <p>除类型编码唯一性校验和悲观锁更新外，还统计配置、原因和事件三类下游引用，
 * 为删除类型时的级联引用保护提供依据。
 */
public interface AndonTypeRepository extends JpaRepository<AndonTypeEntity, Long>,
        JpaSpecificationExecutor<AndonTypeEntity> {

    /** 按主键读取未逻辑删除的类型，供详情展示等只读场景使用。 */
    Optional<AndonTypeEntity> findByIdAndDeletedFalse(Long id);

    /** 以悲观写锁读取活动类型，串行化同一类型的更新、启停和删除操作。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select type from AndonTypeEntity type where type.id = :id and type.deleted = false")
    Optional<AndonTypeEntity> findByIdAndDeletedFalseForUpdate(@Param("id") Long id);

    /** 检查类型编码是否被未删除类型占用，用于新增时的活动数据唯一性校验。 */
    boolean existsByTypeCodeAndDeletedFalse(String typeCode);

    /** 更新类型时排除自身，检查编码是否与其他未删除类型冲突。 */
    boolean existsByTypeCodeAndIdNotAndDeletedFalse(String typeCode, Long id);

    /** 检查类型编码是否曾被使用，包含逻辑删除记录，避免历史编码被无意复用。 */
    boolean existsByTypeCode(String typeCode);

    /** 统计未删除处理配置引用，用于删除类型前的配置级联保护。 */
    @Query(value = "select count(*) from andon_configuration "
            + "where andon_type_id = :typeId and is_deleted = false", nativeQuery = true)
    long countConfigurationsByTypeId(@Param("typeId") Long typeId);

    /** 统计未删除原因引用，用于删除类型前的原因级联保护。 */
    @Query(value = "select count(*) from andon_reason "
            + "where andon_type_id = :typeId and is_deleted = false", nativeQuery = true)
    long countReasonsByTypeId(@Param("typeId") Long typeId);

    /** 统计未删除事件引用；历史事件存在时仍禁止删除其类型主数据。 */
    @Query(value = "select count(*) from andon_event "
            + "where andon_type_id = :typeId and is_deleted = false", nativeQuery = true)
    long countEventsByTypeId(@Param("typeId") Long typeId);
}
