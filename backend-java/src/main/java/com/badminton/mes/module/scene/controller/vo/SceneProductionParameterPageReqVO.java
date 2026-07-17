package com.badminton.mes.module.scene.controller.vo;
import com.badminton.mes.common.core.PageParam;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
/** 生产参数分页请求。 @author 刘涵 */
@Data @EqualsAndHashCode(callSuper = true)
public class SceneProductionParameterPageReqVO extends PageParam {
    @Size(max = 64) private String paramCode;
    @Positive private Long workshopId;
    @Positive private Long lineId;
    @Positive private Long productId;
    @Min(0) @Max(1) private Integer status;
}
