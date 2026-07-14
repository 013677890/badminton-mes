ALTER TABLE equip_maintenance_record
    ADD COLUMN previous_equipment_status VARCHAR(32) DEFAULT NULL
        COMMENT '开始保养前的设备状态，用于任务结束后恢复'
        AFTER equipment_id;
