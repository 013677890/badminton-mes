package com.badminton.mes.module.device.controller.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 设备接入配置创建与修改请求。
 *
 * <p>定义采集点与设备、工序、产线的绑定及计数解释规则。数据来源、初始联调状态和创建时的启用策略
 * 由服务端控制，不允许客户端绕过联调流程直接构造完整持久化状态。
 */
@Data
public class DeviceAccessConfigSaveReqVO {

    /** 设备上报使用的配置编码；系统保留逻辑删除前缀，避免与归档编码冲突。 */
    @NotBlank(message = "接入配置编码不能为空")
    @Size(max = 32, message = "接入配置编码长度不能超过 32")
    @Pattern(regexp = "^(?!__DELETED_).+$", message = "接入配置编码不能使用系统保留前缀")
    private String configCode;

    /** 管理端展示的配置名称。 */
    @NotBlank(message = "接入配置名称不能为空")
    @Size(max = 128, message = "接入配置名称长度不能超过 128")
    private String configName;

    /** 接入采集点绑定的设备台账主键。 */
    @NotNull(message = "设备台账不能为空")
    @Positive(message = "设备台账必须为正整数")
    private Long equipmentId;

    /** 设备内部采集点编码；与设备组合后应保持唯一。 */
    @NotBlank(message = "采集点编码不能为空")
    @Size(max = 64, message = "采集点编码长度不能超过 64")
    @Pattern(regexp = "^(?!__DELETED_).+$", message = "采集点编码不能使用系统保留前缀")
    private String collectionPointCode;

    /** 计数归属工序；为空会使收到的计数进入异常处理。 */
    @Positive(message = "关联工序必须为正整数")
    private Long processId;

    /** 设备所在产线主键。 */
    @Positive(message = "关联产线必须为正整数")
    private Long productionLineId;

    /** 计数模式；累计模式按历史原始值求差，增量模式直接采用本次值。 */
    @Pattern(regexp = "^(CUMULATIVE|INCREMENTAL)$", message = "计数模式必须为 CUMULATIVE 或 INCREMENTAL")
    private String countMode;

    /** 单次有效增量允许的最大值，超过时标记异常跳变。 */
    @Positive(message = "异常跳变阈值必须为正整数")
    private Long spikeThreshold;

    /** 生产任务匹配后的报工策略。 */
    @Pattern(regexp = "^(AUTO|PENDING_CONFIRMATION|NONE)$",
             message = "报工模式必须为 AUTO、PENDING_CONFIRMATION 或 NONE")
    private String reportMode;

    /** 正式采集开关；启用请求仍需满足联调已通过的服务端校验。 */
    @Min(value = 0, message = "正式采集状态只能为 0 或 1")
    @Max(value = 1, message = "正式采集状态只能为 0 或 1")
    private Integer enabledStatus;

    /** 仅用于维护说明，不参与采集与计数规则判断。 */
    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;
}
