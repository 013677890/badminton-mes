package com.badminton.mes.module.production.controller.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 克隆 BOM 新版本请求。 */
@Data
public class BomNewVersionReqVO {
    /** 来源 BOM 的乐观锁版本 */
    @NotNull(message = "来源 BOM 锁版本不能为空")
    @PositiveOrZero(message = "来源 BOM 锁版本不能小于 0")
    private Integer lockVersion;
    /** 新 BOM 编码 */
    @NotBlank(message = "新 BOM 编码不能为空")
    @Size(max = 32, message = "新 BOM 编码长度不能超过 32")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "新 BOM 编码只能包含字母、数字、下划线和连字符")
    private String bomCode;
    /** 新 BOM 业务版本 */
    @NotBlank(message = "新 BOM 版本不能为空")
    @Size(max = 16, message = "新 BOM 版本长度不能超过 16")
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "新 BOM 版本只能包含字母、数字、点、下划线和连字符")
    private String version;
}
