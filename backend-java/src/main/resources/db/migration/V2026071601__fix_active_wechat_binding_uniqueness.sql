ALTER TABLE sys_wechat_user_binding
    DROP INDEX uk_app_open_deleted,
    DROP INDEX uk_app_user_deleted,
    ADD COLUMN active_open_id VARCHAR(128)
        GENERATED ALWAYS AS (
            CASE WHEN is_deleted = 0 AND status = 1 THEN open_id ELSE NULL END
        ) STORED COMMENT '有效绑定 OpenID，供唯一索引使用',
    ADD COLUMN active_user_id BIGINT UNSIGNED
        GENERATED ALWAYS AS (
            CASE WHEN is_deleted = 0 AND status = 1 THEN user_id ELSE NULL END
        ) STORED COMMENT '有效绑定用户主键，供唯一索引使用',
    ADD UNIQUE KEY uk_app_active_open_id (app_id, active_open_id),
    ADD UNIQUE KEY uk_app_active_user_id (app_id, active_user_id);
