package com.badminton.mes.module.scene.controller.vo;
import com.badminton.mes.common.core.PageParam;import jakarta.validation.constraints.*;import lombok.*;
/** 工序作业分页请求。 @author 刘涵 */
@Data @EqualsAndHashCode(callSuper=true)
public class SceneOperationJobPageReqVO extends PageParam {
 @Positive private Long taskId;@Positive private Long userId;@Positive private Long stationId;@Positive private Long equipmentId;
 @Min(0) @Max(3) private Integer detailStatus;
}
