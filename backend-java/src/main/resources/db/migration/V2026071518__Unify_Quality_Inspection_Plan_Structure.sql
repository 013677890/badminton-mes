-- =============================================
-- 统一 quality_inspection_plan 表结构为质量模块契约
-- 作者: 角色B(联调修复)
-- 日期: 2026/07/15
--
-- 背景: V6 先创建了工艺侧最小方案主档(plan_version/status 列)，
-- 导致 V2026071413 的 CREATE TABLE IF NOT EXISTS 被整体跳过，
-- 质量模块实体 QualityInspectionPlanEntity 映射的
-- version_no/plan_status/inspection_type 等列在库中不存在。
-- 该表此前无任何业务写入方(工艺模块仅做只读引用校验)，可安全重建。
-- 工艺模块的引用校验已同步改读 plan_status 列。
-- =============================================

DROP TABLE IF EXISTS quality_inspection_plan;

CREATE TABLE quality_inspection_plan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    plan_code VARCHAR(32) NOT NULL COMMENT '方案编码',
    plan_name VARCHAR(128) NOT NULL COMMENT '方案名称',
    product_id BIGINT DEFAULT NULL COMMENT '适用产品id',
    customer_id BIGINT DEFAULT NULL COMMENT '适用客户id',
    inspection_type VARCHAR(32) NOT NULL COMMENT '检验类型',
    version_no INT NOT NULL DEFAULT 1 COMMENT '版本号',
    plan_status VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT EFFECTIVE DISABLED',
    effective_date DATE DEFAULT NULL COMMENT '生效日期',
    default_flag BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否默认方案',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_by BIGINT NOT NULL COMMENT '创建人用户id',
    audit_by BIGINT DEFAULT NULL COMMENT '审核人用户id',
    audit_time DATETIME DEFAULT NULL COMMENT '审核时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE COMMENT '逻辑删除标记',
    UNIQUE KEY uk_quality_plan_code_version (plan_code, version_no),
    INDEX idx_quality_plan_scope (product_id, customer_id, inspection_type, plan_status, is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='质量检验标准方案表';
