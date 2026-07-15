package com.badminton.mes.module.scene.controller.vo;
import com.badminton.mes.common.core.PageParam;import jakarta.validation.constraints.*;import lombok.*;
/** 产品状态分页请求。 @author 刘涵 */
@Data @EqualsAndHashCode(callSuper=true)
public class SceneProductStatusPageReqVO extends PageParam {
 @Size(max=64) private String batchNo;@Positive private Long taskId;@Min(1) @Max(6) private Integer batchStatus;
 private Boolean abnormal;
}
