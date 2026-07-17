package com.badminton.mes.module.system.controller.vo;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 小程序管理员调整用户职位与生产组织请求。
 *
 * @author Codex
 * @date 2026/07/16
 */
@Data
public class UserAssignmentReqVO {

    /** 目标职位主键列表；ADMIN 是否可修改由当前登录用户权限决定。 */
    @NotNull(message = "职位列表不能为空")
    @Size(max = 10, message = "职位数量不能超过 10 个")
    private List<@Positive(message = "角色 id 必须为正数") Long> roleIds;

    /** 所属车间主键，可空。 */
    @Positive(message = "车间 id 必须为正数")
    private Long workshopId;

    /** 所属产线主键，可空。 */
    @Positive(message = "产线 id 必须为正数")
    private Long lineId;
}
