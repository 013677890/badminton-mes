package com.badminton.mes.module.andon.controller.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 安灯异常原因创建/修改请求。
 *
 * <p>原因必须归属于一个安灯类型，事件发起或确认时服务层据此校验原因与类型一致；
 * 编码承担稳定业务标识，名称和描述承担现场选择及统计解释。
 */
@Data
public class AndonReasonSaveReqVO {

    /** 原因业务编码；必须唯一，且不得使用软删除流程保留的 {@code __DELETED_} 前缀。 */
    @NotBlank(message = "异常原因编码不能为空")
    @Size(max = 32, message = "异常原因编码长度不能超过 32")
    @Pattern(regexp = "^(?!__DELETED_).+$", message = "异常原因编码不能使用系统保留前缀")
    private String reasonCode;

    /** 原因显示名称，应采用现场人员可识别的异常归因表述。 */
    @NotBlank(message = "异常原因名称不能为空")
    @Size(max = 128, message = "异常原因名称长度不能超过 128")
    private String reasonName;

    /** 所属安灯类型主键；决定该原因可用于哪些事件并接受类型有效性校验。 */
    @NotNull(message = "安灯类型不能为空")
    @Positive(message = "安灯类型必须为正整数")
    private Long andonTypeId;

    /** 原因判定标准、典型现象或适用边界的补充说明。 */
    @Size(max = 500, message = "原因描述长度不能超过 500")
    private String reasonDescription;

    /** 启用状态：{@code 1} 允许用于新事件，{@code 0} 仅保留主数据和历史引用。 */
    @Min(value = 0, message = "启用状态只能为 0 或 1")
    @Max(value = 1, message = "启用状态只能为 0 或 1")
    private Integer enabledStatus;
}
