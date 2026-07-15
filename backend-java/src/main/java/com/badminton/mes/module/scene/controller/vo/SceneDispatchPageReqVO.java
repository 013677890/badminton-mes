package com.badminton.mes.module.scene.controller.vo;
import com.badminton.mes.common.core.PageParam;
import jakarta.validation.constraints.*;
import lombok.*;
/** 派工分页请求。 @author 刘涵 */
@Data @EqualsAndHashCode(callSuper=true)
public class SceneDispatchPageReqVO extends PageParam {
    @Size(max=32) private String dispatchNo;@Positive private Long taskId;@Min(0) @Max(4) private Integer dispatchStatus;
}
