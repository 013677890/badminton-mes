package com.badminton.mes.module.barcode.dal.repository;

import java.util.List;
import java.util.Optional;

import com.badminton.mes.module.barcode.dal.entity.BarcodeTemplateEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 条码模板 JPA Repository。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
public interface BarcodeTemplateRepository extends JpaRepository<BarcodeTemplateEntity, Long>,
        JpaSpecificationExecutor<BarcodeTemplateEntity> {

    /**
     * 按主键查询未删除的条码模板。
     *
     * @param id 模板主键
     * @return 模板实体
     */
    Optional<BarcodeTemplateEntity> findByIdAndDeletedFalse(Long id);

    /**
     * 判断未删除模板中是否已存在指定编码(任意版本)，创建时查重。
     *
     * @param templateCode 模板编码
     * @return true 存在，false 不存在
     */
    boolean existsByTemplateCodeAndDeletedFalse(String templateCode);

    /**
     * 查询同编码全部未删除版本行，升版本时计算下一版本号。
     *
     * @param templateCode 模板编码
     * @return 版本行列表
     */
    List<BarcodeTemplateEntity> findByTemplateCodeAndDeletedFalse(String templateCode);

    /**
     * 就地修改模板基础信息(未被绑定时)，模板编码与版本不变。
     *
     * @param id           模板主键
     * @param templateName 模板名称
     * @param paperWidth   纸张宽度
     * @param paperHeight  纸张高度
     * @return 影响行数；0 表示模板不存在或已删除
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE BarcodeTemplateEntity template
            SET template.templateName = :templateName,
                template.paperWidth = :paperWidth,
                template.paperHeight = :paperHeight,
                template.updateTime = CURRENT_TIMESTAMP
            WHERE template.id = :id
              AND template.deleted = false
            """)
    int updateInfo(@Param("id") Long id,
                   @Param("templateName") String templateName,
                   @Param("paperWidth") java.math.BigDecimal paperWidth,
                   @Param("paperHeight") java.math.BigDecimal paperHeight);

    /**
     * 状态流转 CAS：仅当当前状态等于前置状态才更新，用于启用/停用。
     *
     * @param id         模板主键
     * @param fromStatus 前置状态
     * @param toStatus   目标状态
     * @return 影响行数；0 表示模板不存在、已删除或状态不满足
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE BarcodeTemplateEntity template
            SET template.status = :toStatus,
                template.updateTime = CURRENT_TIMESTAMP
            WHERE template.id = :id
              AND template.status = :fromStatus
              AND template.deleted = false
            """)
    int updateStatus(@Param("id") Long id,
                     @Param("fromStatus") Integer fromStatus,
                     @Param("toStatus") Integer toStatus);
}
