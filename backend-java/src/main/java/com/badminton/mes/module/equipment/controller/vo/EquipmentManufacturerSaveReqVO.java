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
 * <p>注解负责必填、长度以及电话、邮箱、官网格式等单字段校验。制造商编码唯一性和修改记录存在性
 * 需要访问持久层，由 Service 负责；可选联系方式为空时不触发对应格式约束。
 *
 * @author 角色C
 * @date 2026/07/09
 */
@Data
public class EquipmentManufacturerSaveReqVO {

    /** 业务唯一的制造商编码，不能为空，最大 32 个字符。 */
    @NotBlank(message = "制造商编码不能为空")
    @Size(max = 32, message = "制造商编码长度不能超过 32")
    private String manufacturerCode;

    /** 制造商正式名称，供设备台账选择与业务展示，最大 128 个字符。 */
    @NotBlank(message = "制造商名称不能为空")
    @Size(max = 128, message = "制造商名称长度不能超过 128")
    private String manufacturerName;

    /** 制造商业务联系人，可空，最大 64 个字符。 */
    @Size(max = 64, message = "联系人长度不能超过 64")
    private String contactPerson;

    /** 联系电话，可空；仅允许数字、空白及常用的分机或国际区号分隔符。 */
    @Size(max = 32, message = "联系电话长度不能超过 32")
    @Pattern(regexp = "^[0-9\\-+()\\s]*$", message = "联系电话格式不正确")
    private String contactPhone;

    /** 联系邮箱，可空；非空时必须符合邮箱格式且不超过 64 个字符。 */
    @Size(max = 64, message = "联系邮箱长度不能超过 64")
    @Email(message = "邮箱格式不正确")
    private String contactEmail;

    /** 制造商办公地址或通信地址，可空，最大 255 个字符。 */
    @Size(max = 255, message = "地址长度不能超过 255")
    private String address;

    /** 官方网站地址，可省略 HTTP 协议；非空时必须包含合法域名后缀。 */
    @Size(max = 128, message = "官网长度不能超过 128")
    @Pattern(regexp = "^(https?://)?[\\w\\-.]+\\.[a-z]{2,}.*$", message = "官网格式不正确", 
             flags = Pattern.Flag.CASE_INSENSITIVE)
    private String website;

    /** 合作关系、供货范围等补充说明，可空，最大 255 个字符。 */
    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;

    /** 启停状态：1 表示启用，0 表示停用；可空并由 Service 补充默认值 1。 */
    @Min(value = 0, message = "状态取值为 0 或 1")
    @Max(value = 1, message = "状态取值为 0 或 1")
    private Integer status;
}
