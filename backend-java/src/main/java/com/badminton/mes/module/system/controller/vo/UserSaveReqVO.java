package com.badminton.mes.module.system.controller.vo;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户创建/修改请求 VO。
 *
 * <p>创建与修改共用：password 仅创建时生效(必填由 Service 校验，修改忽略)；
 * 修改时 userNo 被忽略，工号不允许变更；密码变更走专用接口。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@Data
public class UserSaveReqVO {

    /** 工号 */
    @NotBlank(message = "工号不能为空")
    @Size(max = 32, message = "工号长度不能超过 32")
    private String userNo;

    /** 姓名 */
    @NotBlank(message = "姓名不能为空")
    @Size(max = 64, message = "姓名长度不能超过 64")
    private String userName;

    /** 初始密码，仅创建时生效 */
    @Size(min = 6, max = 32, message = "密码长度必须在 6 到 32 之间")
    private String password;

    /** 手机号，可空；填写时须为 11 位大陆手机号 */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String mobile;

    /** 所属车间 id */
    @Positive(message = "车间 id 必须为正数")
    private Long workshopId;

    /** 所属产线 id */
    @Positive(message = "产线 id 必须为正数")
    private Long lineId;

    /** 角色 id 列表，至少一个 */
    @NotEmpty(message = "至少分配一个角色")
    private List<Long> roleIds;
}
