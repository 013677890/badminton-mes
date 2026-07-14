-- ----------------------------------------------------------------------------
-- V10 外部接口写入日志补齐更新时间审计字段
-- ----------------------------------------------------------------------------

ALTER TABLE `integration_write_log`
  ADD COLUMN `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER `create_time`;
