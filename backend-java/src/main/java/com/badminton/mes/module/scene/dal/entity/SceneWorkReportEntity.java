package com.badminton.mes.module.scene.dal.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;

/** 生产报工及反向冲销事实。 @author 刘涵 */
@Data @Entity @DynamicInsert @Table(name = "prod_report")
public class SceneWorkReportEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name="report_no") private String reportNo;
    @Column(name="request_no") private String requestNo;
    @Column(name="task_id") private Long taskId;
    @Column(name="dispatch_detail_id") private Long dispatchDetailId;
    @Column(name="process_id") private Long processId;
    @Column(name="batch_no") private String batchNo;
    @Column(name="barcode_id") private Long barcodeId;
    @Column(name="report_type") private Integer reportType;
    @Column(name="record_type") private Integer recordType;
    @Column(name="source_report_id") private Long sourceReportId;
    @Column(name="user_id") private Long userId;
    @Column(name="equipment_id") private Long equipmentId;
    @Column(name="station_id") private Long stationId;
    @Column(name="input_quantity") private Integer inputQuantity;
    @Column(name="good_quantity") private Integer goodQuantity;
    @Column(name="defect_quantity") private Integer defectQuantity;
    @Column(name="rework_quantity") private Integer reworkQuantity;
    @Column(name="source_type") private Integer sourceType;
    @Column(name="reverse_reason") private String reverseReason;
    @Column(name="report_time") private LocalDateTime reportTime;
    @Column(name="create_time",insertable=false,updatable=false) private LocalDateTime createTime;
    @Column(name="update_time",insertable=false) private LocalDateTime updateTime;
    @Column(name="is_deleted") private Boolean deleted;
}
