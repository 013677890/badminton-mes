CREATE TABLE sys_wechat_user_binding (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT UNSIGNED NOT NULL COMMENT 'MES 用户主键',
    app_id VARCHAR(64) NOT NULL COMMENT '微信小程序 AppID',
    open_id VARCHAR(128) NOT NULL COMMENT '微信 OpenID',
    status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 停用',
    last_login_time DATETIME NULL COMMENT '最近登录时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 否，1 是',
    PRIMARY KEY pk_id (id),
    UNIQUE KEY uk_app_open_deleted (app_id, open_id, is_deleted),
    UNIQUE KEY uk_app_user_deleted (app_id, user_id, is_deleted),
    KEY idx_user_id (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '微信小程序用户绑定';
