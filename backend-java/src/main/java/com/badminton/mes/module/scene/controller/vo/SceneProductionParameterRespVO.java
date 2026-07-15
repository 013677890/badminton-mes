package com.badminton.mes.module.scene.controller.vo;
import java.time.LocalDateTime;
import lombok.Data;
/** 生产参数响应。 @author 刘涵 */
@Data
public class SceneProductionParameterRespVO {
    private Long id; private String paramCode; private String paramName; private String paramValue;
    private Integer valueType; private Long workshopId; private Long lineId; private Long productId;
    private String remark; private Integer status; private LocalDateTime createTime; private LocalDateTime updateTime;
}
