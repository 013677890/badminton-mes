package com.badminton.mes.module.equipment.controller.vo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 设备制造商创建/修改请求 VO。
 *
 * <p>对外开放接口必须做入参校验，单字段规则用注解声明；
 * 跨字段业务规则在 Service 层校验。
 *
 * @author 角色C
 * @date 2026/07/09
 */
@Data
public class EquipmentManufacturerSaveReqVO {

    /** 制造商编码，唯一 */
    @NotBlank(message = "制造商编码不能为空")
    @Size(max = 32, message = "制造商编码长度不能超过 32")
    private String manufacturerCode;

    /** 制造商名称 */
    @NotBlank(message = "制造商名称不能为空")
    @Size(max = 128, message = "制造商名称长度不能超过 128")
    private String manufacturerName;

    /** 联系人，可空 */
    @Size(max = 64, message = "联系人长度不能超过 64")
    private String contactPerson;

    /** 联系电话，可空 */
    @Size(max = 32, message = "联系电话长度不能超过 32")
    @Pattern(regexp = "^[0-9\\-+()\\s]*$", message = "联系电话格式不正确")
    private String contactPhone;

    /** 联系邮箱，可空 */
    @Size(max = 64, message = "联系邮箱长度不能超过 64")
    @Email(message = "邮箱格式不正确")
    private String contactEmail;

    /** 地址，可空 */
    @Size(max = 255, message = "地址长度不能超过 255")
    private String address;

    /** 官网，可空 */
    @Size(max = 128, message = "官网长度不能超过 128")
    @Pattern(regexp = "^(https?://)?[\\w\\-.]+\\.[a-z]{2,}.*$", message = "官网格式不正确", 
             flags = Pattern.Flag.CASE_INSENSITIVE)
    private String website;

    /** 备注说明，可空 */
    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;

    /** 状态：1 启用 0 停用，可空，默认 1 */
    @Min(value = 0, message = "状态取值为 0 或 1")
    @Max(value = 1, message = "状态取值为 0 或 1")
    private Integer status;
}
