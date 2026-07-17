package com.badminton.mes.module.scene.controller.vo;
import java.time.*;
import jakarta.validation.constraints.*;
import lombok.Data;
/** 生产任务保存请求。 @author 刘涵 */
@Data
public class SceneProductionTaskSaveReqVO {
    @NotNull @Positive private Long workOrderId;
    @NotNull @Positive private Long lineId;
    @Positive private Long shiftId;
    @NotNull private LocalDate planDate;
    @NotNull @Positive private Integer planQuantity;
    @NotNull private LocalDateTime planStartTime;
    @NotNull private LocalDateTime planEndTime;
}
