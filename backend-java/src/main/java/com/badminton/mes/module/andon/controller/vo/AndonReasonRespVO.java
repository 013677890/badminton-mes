package com.badminton.mes.module.andon.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 安灯异常原因详情响应。
 *
 * <p>返回原因自身信息及所属安灯类型的编码、名称，便于客户端无需额外查询即可展示原因分类，
 * 同时通过启用状态区分可用于新事件的原因与仅供历史记录追溯的原因。
 */
@Data
public class AndonReasonRespVO {

    /** 原因数据库主键，用于事件预判原因和实际原因关联。 */
    private Long id;
    /** 原因业务编码，在所属业务中作为稳定、可读的原因标识。 */
    private String reasonCode;
    /** 原因显示名称，供发起、确认和统计界面使用。 */
    private String reasonName;
    /** 所属安灯类型主键，限制该原因只能用于对应类型的事件。 */
    private Long andonTypeId;
    /** 所属安灯类型编码，供客户端按稳定编码识别分类。 */
    private String andonTypeCode;
    /** 所属安灯类型名称，供列表和详情直接展示。 */
    private String andonTypeName;
    /** 原因适用场景、判断依据或补充定义。 */
    private String reasonDescription;
    /** 启用状态：{@code 1} 可供新事件选择，{@code 0} 停用但保留历史关联。 */
    private Integer enabledStatus;

    /** 原因记录创建时间，按 {@code yyyy-MM-dd HH:mm:ss} 输出。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 原因名称、归属、描述或启用状态最后修改时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
