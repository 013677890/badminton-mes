package com.badminton.mes.module.production.controller.vo;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** BOM 草稿创建与修改请求。 */
@Data
public class BomSaveReqVO {
    /** BOM 编码 */
    @NotBlank(message = "BOM 编码不能为空")
    @Size(max = 32, message = "BOM 编码长度不能超过 32")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "BOM 编码只能包含字母、数字、下划线和连字符")
    private String bomCode;
    /** 产品主键 */
    @NotNull(message = "产品不能为空")
    @Positive(message = "产品 id 必须为正数")
    private Long productId;
    /** BOM 业务版本 */
    @NotBlank(message = "BOM 版本不能为空")
    @Size(max = 16, message = "BOM 版本长度不能超过 16")
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "BOM 版本只能包含字母、数字、点、下划线和连字符")
    private String version;
    /** BOM 物料明细 */
    @NotEmpty(message = "BOM 明细不能为空")
    @Size(max = 200, message = "单个 BOM 最多包含 200 条物料明细")
    private List<@NotNull @Valid BomDetailSaveReqVO> details;
}
