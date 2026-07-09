-- ----------------------------------------------------------------------------
-- V3 认证与权限管理：系统用户、角色、用户角色关系 + 种子数据
--
-- sys_user / sys_role / sys_user_role 照抄 wiki/database/mes_schema.sql。
-- 业务关系由 Service 校验与应用层约束处理，不建实体外键；
-- 唯一约束和查询索引仍在数据库层兜底。
-- 设计说明见 wiki/15-认证与权限管理设计.md。
-- ----------------------------------------------------------------------------

-- 系统用户表
CREATE TABLE `sys_user` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_no` varchar(32) NOT NULL COMMENT '工号(唯一)',
  `user_name` varchar(64) NOT NULL COMMENT '姓名',
  `password` varchar(128) NOT NULL COMMENT '密码(加密存储)',
  `mobile` varchar(20) NULL DEFAULT NULL COMMENT '手机号',
  `workshop_id` bigint unsigned NULL DEFAULT NULL COMMENT '所属车间',
  `line_id` bigint unsigned NULL DEFAULT NULL COMMENT '所属产线(操作工/班组长)',
  `status` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除:1删除 0未删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_no` (`user_no`) COMMENT '工号唯一',
  KEY `idx_workshop_id` (`workshop_id`) COMMENT '按车间查人员',
  KEY `idx_line_id` (`line_id`) COMMENT '按产线查班组/操作工'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '系统用户表';

-- 系统角色表
CREATE TABLE `sys_role` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_code` varchar(32) NOT NULL COMMENT '角色编码(唯一)',
  `role_name` varchar(64) NOT NULL COMMENT '角色名称(管理员/PMC/车间主管/班组长/操作工/质检员等)',
  `remark` varchar(255) NULL DEFAULT NULL COMMENT '备注',
  `status` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`) COMMENT '角色编码唯一'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '系统角色表';

-- 用户角色关系表
CREATE TABLE `sys_user_role` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `role_id` bigint unsigned NOT NULL COMMENT '角色ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`) COMMENT '防重复授权(INDEX-001)',
  KEY `idx_role_id` (`role_id`) COMMENT '按角色反查用户(安灯按角色匹配处理人)'
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '用户角色关系表';

-- 内置角色种子数据(角色作为固定档案，不提供增删改接口)
INSERT INTO `sys_role` (`role_code`, `role_name`, `remark`, `status`) VALUES
('ADMIN', '管理员', '系统管理，用户与角色维护', 1),
('PMC', 'PMC计划员', '生产计划与工单管理', 1),
('WORKSHOP_MANAGER', '车间主管', '车间生产执行管理', 1),
('TEAM_LEADER', '班组长', '班组派工与现场作业', 1),
('OPERATOR', '操作工', '工位报工与作业执行', 1),
('INSPECTOR', '质检员', '检验与质量记录', 1);

-- 内置管理员(初始密码 admin123 的 BCrypt 哈希，已用 BCrypt.checkpw 验证通过；首次登录后应修改)
INSERT INTO `sys_user` (`user_no`, `user_name`, `password`, `status`) VALUES
('admin', '系统管理员', '$2a$10$X0Q0Kv1hVbdfGgS4u2G.5ODhaDgxFMAXmXRxaDkLhbACOSq/k9Up2', 1);

INSERT INTO `sys_user_role` (`user_id`, `role_id`)
SELECT u.`id`, r.`id`
FROM `sys_user` u, `sys_role` r
WHERE u.`user_no` = 'admin' AND r.`role_code` = 'ADMIN';
