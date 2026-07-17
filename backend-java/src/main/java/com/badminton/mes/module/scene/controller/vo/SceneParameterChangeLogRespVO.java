package com.badminton.mes.module.scene.controller.vo;
import java.time.LocalDateTime;
import lombok.Data;
/** 生产参数变更日志响应。 @author 刘涵 */
@Data
public class SceneParameterChangeLogRespVO {
    private Long id; private String beforeValue; private String afterValue; private Integer beforeStatus;
    private Integer afterStatus; private String changeReason; private Long operatorId; private LocalDateTime operateTime;
}
