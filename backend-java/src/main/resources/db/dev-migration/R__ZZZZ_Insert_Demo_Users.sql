-- 可登录的演示角色。密码均为 admin123（仅限本地开发/课程演示环境）。
INSERT INTO sys_user (user_no, user_name, password, status)
VALUES
  ('pmc_demo', '演示计划员', '$2a$10$X0Q0Kv1hVbdfGgS4u2G.5ODhaDgxFMAXmXRxaDkLhbACOSq/k9Up2', 1),
  ('manager_demo', '演示车间主管', '$2a$10$X0Q0Kv1hVbdfGgS4u2G.5ODhaDgxFMAXmXRxaDkLhbACOSq/k9Up2', 1),
  ('leader_demo', '演示班组长', '$2a$10$X0Q0Kv1hVbdfGgS4u2G.5ODhaDgxFMAXmXRxaDkLhbACOSq/k9Up2', 1),
  ('operator_demo', '演示操作工', '$2a$10$X0Q0Kv1hVbdfGgS4u2G.5ODhaDgxFMAXmXRxaDkLhbACOSq/k9Up2', 1),
  ('inspector_demo', '演示质检员', '$2a$10$X0Q0Kv1hVbdfGgS4u2G.5ODhaDgxFMAXmXRxaDkLhbACOSq/k9Up2', 1),
  ('craft_demo', '演示工艺工程师', '$2a$10$X0Q0Kv1hVbdfGgS4u2G.5ODhaDgxFMAXmXRxaDkLhbACOSq/k9Up2', 1)
ON DUPLICATE KEY UPDATE user_name = VALUES(user_name), password = VALUES(password), status = 1, is_deleted = 0;

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u
JOIN sys_role r ON r.role_code = CASE u.user_no
  WHEN 'pmc_demo' THEN 'PMC'
  WHEN 'manager_demo' THEN 'WORKSHOP_MANAGER'
  WHEN 'leader_demo' THEN 'TEAM_LEADER'
  WHEN 'operator_demo' THEN 'OPERATOR'
  WHEN 'inspector_demo' THEN 'INSPECTOR'
  WHEN 'craft_demo' THEN 'CRAFT_ENGINEER'
END
WHERE u.user_no IN ('pmc_demo', 'manager_demo', 'leader_demo', 'operator_demo', 'inspector_demo', 'craft_demo')
ON DUPLICATE KEY UPDATE is_deleted = 0;
