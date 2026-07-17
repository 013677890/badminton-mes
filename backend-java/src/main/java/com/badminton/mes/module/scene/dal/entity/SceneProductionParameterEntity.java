package com.badminton.mes.module.scene.dal.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;

/** 生产参数实体。 @author 刘涵 */
@Data @Entity @DynamicInsert @Table(name = "prod_param")
public class SceneProductionParameterEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "param_code") private String paramCode;
    @Column(name = "param_name") private String paramName;
    @Column(name = "param_value") private String paramValue;
    @Column(name = "value_type", columnDefinition = "tinyint unsigned") private Integer valueType;
    @Column(name = "workshop_id") private Long workshopId;
    @Column(name = "line_id") private Long lineId;
    @Column(name = "product_id") private Long productId;
    private String remark;
    @Column(columnDefinition = "tinyint unsigned") private Integer status;
    @Column(name = "create_by") private Long createBy;
    @Column(name = "create_time", insertable = false, updatable = false) private LocalDateTime createTime;
    @Column(name = "update_time", insertable = false) private LocalDateTime updateTime;
    @Column(name = "is_deleted", columnDefinition = "tinyint unsigned") private Boolean deleted;
}
