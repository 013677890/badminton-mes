package com.badminton.mes.module.scene.controller.vo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
/** 当前生效参数查询请求。 @author 刘涵 */
@Data
public class SceneEffectiveParameterReqVO {
    @NotBlank @Size(max = 64) private String paramCode;
    @Positive private Long workshopId;
    @Positive private Long lineId;
    @Positive private Long productId;
}
