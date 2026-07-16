package com.badminton.mes.module.equipment.dal.repository;

import java.util.Optional;

import com.badminton.mes.module.equipment.dal.entity.EquipmentManufacturerEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * 设备制造商 JPA Repository。
 *
 * <p>继承基础 CRUD 与动态规格执行能力，并为详情读取和编码唯一性校验提供带逻辑删除过滤的
 * 派生查询。接口不声明锁；只读查询返回当前有效档案，由 Service 负责更新事务和引用校验。
 *
 * @author 角色C
 * @date 2026/07/09
 */
public interface EquipmentManufacturerRepository extends JpaRepository<EquipmentManufacturerEntity, Long>,
        JpaSpecificationExecutor<EquipmentManufacturerEntity> {

    /**
     * 按主键读取未逻辑删除的制造商，供详情展示及普通业务校验使用。
     *
     * <p>派生条件固定追加 {@code deleted = false}，不加数据库锁；不存在或已删除时返回空值。
     *
     * @param id 制造商主键
     * @return 当前有效的制造商实体；不存在或已逻辑删除时为 {@link Optional#empty()}
     */
    Optional<EquipmentManufacturerEntity> findByIdAndDeletedFalse(Long id);

    /**
     * 判断有效制造商中是否已占用指定编码，供新增前执行业务唯一性校验。
     *
     * <p>仅统计 {@code deleted = false} 的档案，不加锁；数据库唯一约束仍是并发写入的最终保障。
     *
     * @param manufacturerCode 制造商编码
     * @return {@code true} 表示已有有效档案占用该编码，否则为 {@code false}
     */
    boolean existsByManufacturerCodeAndDeletedFalse(String manufacturerCode);

    /**
     * 判断除当前档案外是否有其他有效制造商占用指定编码，供修改编码时防重。
     *
     * <p>同时应用 {@code id != 指定值} 和 {@code deleted = false}，因此当前记录自身不会造成误报；
     * 查询不加数据库锁，由数据库唯一约束兜底并发写入竞争。
     *
     * @param manufacturerCode 制造商编码
     * @param id               排除的制造商 id
     * @return {@code true} 表示其他有效档案已占用该编码，否则为 {@code false}
     */
    boolean existsByManufacturerCodeAndIdNotAndDeletedFalse(String manufacturerCode, Long id);
}
