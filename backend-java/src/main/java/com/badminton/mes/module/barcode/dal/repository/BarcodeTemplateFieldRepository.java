package com.badminton.mes.module.barcode.dal.repository;

import java.util.List;

import com.badminton.mes.module.barcode.dal.entity.BarcodeTemplateFieldEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 条码模板字段 JPA Repository。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
public interface BarcodeTemplateFieldRepository extends JpaRepository<BarcodeTemplateFieldEntity, Long> {

    /**
     * 查询模板的未删除字段配置，按主键升序稳定输出。
     *
     * @param templateId 模板主键
     * @return 字段配置列表
     */
    List<BarcodeTemplateFieldEntity> findByTemplateIdAndDeletedFalseOrderByIdAsc(Long templateId);

    /**
     * 按模板逻辑删除全部字段，就地修改重写字段时使用。
     *
     * @param templateId 模板主键
     * @return 影响行数
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE BarcodeTemplateFieldEntity field
            SET field.deleted = true,
                field.updateTime = CURRENT_TIMESTAMP
            WHERE field.templateId = :templateId
              AND field.deleted = false
            """)
    int logicDeleteByTemplateId(@Param("templateId") Long templateId);
}
