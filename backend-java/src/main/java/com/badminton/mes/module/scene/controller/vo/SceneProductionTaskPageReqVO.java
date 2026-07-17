package com.badminton.mes.module.scene.controller.vo;
import java.time.LocalDate;
import com.badminton.mes.common.core.PageParam;
import jakarta.validation.constraints.*;
import lombok.*;
/** 生产任务分页请求。 @author 刘涵 */
@Data @EqualsAndHashCode(callSuper=true)
public class SceneProductionTaskPageReqVO extends PageParam {
    @Size(max=32) private String taskNo; @Positive private Long workshopId; @Positive private Long lineId;
    @Min(0) @Max(7) private Integer taskStatus; private LocalDate planDate;
}
