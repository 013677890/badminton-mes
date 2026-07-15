package com.badminton.mes.module.scene.dal.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;

/** 生产完工单。 @author 刘涵 */
@Data @Entity @DynamicInsert @Table(name="prod_finish_order")
public class SceneCompletionOrderEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="finish_no") private String finishNo;
    @Column(name="task_id") private Long taskId;
    @Column(name="work_order_id") private Long workOrderId;
    @Column(name="product_id") private Long productId;
    @Column(name="batch_no") private String batchNo;
    @Column(name="finish_quantity") private Integer finishQuantity;
    @Column(name="good_quantity") private Integer goodQuantity;
    @Column(name="defect_quantity") private Integer defectQuantity;
    @Column(name="rework_quantity") private Integer reworkQuantity;
    @Column(name="finish_status") private Integer finishStatus;
    @Column(name="audit_by") private Long auditBy;
    @Column(name="audit_time") private LocalDateTime auditTime;
    @Column(name="audit_remark") private String auditRemark;
    @Column(name="sync_status") private Integer syncStatus;
    @Column(name="create_by") private Long createBy;
    @Column(name="create_time",insertable=false,updatable=false) private LocalDateTime createTime;
    @Column(name="update_time",insertable=false) private LocalDateTime updateTime;
    @Column(name="is_deleted") private Boolean deleted;
}
