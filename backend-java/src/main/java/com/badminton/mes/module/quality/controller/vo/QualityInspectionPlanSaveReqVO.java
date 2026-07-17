package com.badminton.mes.module.quality.controller.vo;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 检验标准方案草稿创建/修改请求。
 *
 * <p>该对象描述一个完整方案草稿及其项目集合，而不是对已生效版本做局部补丁。服务层仅允许草稿执行修改，
 * 并校验项目引用有效性、项目不重复以及规则覆盖组合；审核后若需变更，应从原版本复制出新草稿。
 */
@Data
public class QualityInspectionPlanSaveReqVO {

    /** 方案业务编码；同一版本链共享该编码，创建后不允许通过普通修改切换版本链。 */
    @NotBlank(message = "方案编码不能为空")
    @Size(max = 32, message = "方案编码长度不能超过 32")
    private String planCode;

    /** 方案名称，用于维护界面和检验单选用时识别标准。 */
    @NotBlank(message = "方案名称不能为空")
    @Size(max = 128, message = "方案名称长度不能超过 128")
    private String planName;

    /** 可选的适用产品主键；填写后，建单产品必须与其一致。 */
    @Positive(message = "适用产品必须为正整数")
    private Long productId;

    /** 可选的适用客户主键；用于限制客户专用标准的选用范围。 */
    @Positive(message = "适用客户必须为正整数")
    private Long customerId;

    /** 方案检验类型；必须与后续检验单入口传入的类型完全一致。 */
    @NotBlank(message = "检验类型不能为空")
    @Pattern(regexp = "^(FIRST_ARTICLE|LAST_ARTICLE|PATROL|WAREHOUSE_IN|SHIPMENT)$",
             message = "检验类型不合法")
    private String inspectionType;

    /** 计划生效日期；审核时若为空，服务层使用审核当日补齐。 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate effectiveDate;

    /** 是否作为相同产品、客户和检验类型范围内的默认方案；审核阶段校验唯一性。 */
    private Boolean defaultFlag;

    /** 版本建立原因、适用例外或其他质量管理说明。 */
    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;

    /**
     * 方案采用的检验项目集合。
     *
     * <p>同一项目只能出现一次；保存时会把项目默认规则与本列表中的覆盖值合成为方案项快照。
     */
    @Valid
    @NotEmpty(message = "检验方案至少包含一个检验项目")
    @Size(max = 100, message = "单个方案最多包含 100 个检验项目")
    private List<QualityInspectionPlanItemSaveReqVO> items;
}
