-- ----------------------------------------------------------------------------
-- V11 计件工资结算数量合计扩容
-- ----------------------------------------------------------------------------

ALTER TABLE `wage_settlement`
  MODIFY COLUMN `total_qualified_quantity` decimal(18,4) NOT NULL DEFAULT 0 COMMENT '合格数量合计',
  MODIFY COLUMN `total_defect_quantity` decimal(18,4) NOT NULL DEFAULT 0 COMMENT '不良数量合计';
