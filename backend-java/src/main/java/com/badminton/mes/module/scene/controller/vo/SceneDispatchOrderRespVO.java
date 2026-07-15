package com.badminton.mes.module.scene.controller.vo;
import java.util.List;
import lombok.Data;
/** 派工单响应。 @author 刘涵 */
@Data
public class SceneDispatchOrderRespVO {
    private Long id;private String dispatchNo;private Long taskId;private Long routingId;private String routingCode;
    private String routingVersion;private Integer dispatchStatus;private List<SceneDispatchDetailRespVO> operations;
}
