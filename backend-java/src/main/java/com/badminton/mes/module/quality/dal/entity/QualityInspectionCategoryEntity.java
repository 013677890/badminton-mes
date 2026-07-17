package com.badminton.mes.module.quality.dal.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.DynamicInsert;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 质量检验项目分类实体。
 *
 * <p>分类是检验项目的上层业务目录，用于按检测对象或专业领域组织项目。检验项目通过
 * {@code categoryId} 引用本实体；分类本身不承载判定阈值，阈值和判定方式由具体检验项目定义。
 * 分类被项目引用时不能删除，分类状态发生变化时还需要级联失效相关项目详情缓存，避免页面继续展示
 * 已停用分类下的旧项目视图。
 */
@Data
@Entity
@DynamicInsert
@Table(name = "quality_inspection_category")
public class QualityInspectionCategoryEntity {

    /** 分类数据库主键，也是检验项目建立归属关系时保存的引用值。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 业务侧稳定的分类编码；逻辑删除时会被改写，以释放原编码供后续重新使用。 */
    @Column(name = "category_code")
    private String categoryCode;

    /** 面向维护人员展示的分类名称。 */
    @Column(name = "category_name")
    private String categoryName;

    /** 启停状态：启用分类可供项目创建或调整归属，停用分类不得再作为有效引用目标。 */
    @Column(name = "enabled_status")
    private Integer enabledStatus;

    /** 分类适用边界、管理约定等补充说明。 */
    @Column(name = "remark")
    private String remark;

    /** 创建操作人标识，用于追溯基础资料的建立责任。 */
    @Column(name = "create_by")
    private Long createBy;

    /** 数据库生成的创建时间，应用层只读。 */
    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    /** 数据库维护的最后更新时间，应用层只读。 */
    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;

    /** 逻辑删除标记；查询和唯一性校验通常只面向未删除数据。 */
    @Column(name = "is_deleted")
    private Boolean deleted;
}
