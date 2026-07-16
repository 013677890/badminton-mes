package com.badminton.mes.module.system.controller.vo;

import com.badminton.mes.common.core.PageParam;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户分页查询请求 VO。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserPageReqVO extends PageParam {

    /** 工号或姓名关键字，两个字段使用 OR 前缀匹配。 */
    @Size(max = 64, message = "关键字长度不能超过 64")
    private String keyword;

    /** 工号，前缀匹配 */
    @Size(max = 32, message = "工号长度不能超过 32")
    private String userNo;

    /** 姓名，前缀匹配 */
    @Size(max = 64, message = "姓名长度不能超过 64")
    private String userName;

    /** 所属车间 id */
    @Positive(message = "车间 id 必须为正数")
    private Long workshopId;

    /** 角色 id，筛选拥有该角色的用户 */
    @Positive(message = "角色 id 必须为正数")
    private Long roleId;

    /** 状态：1 启用 0 停用 */
    private Integer status;

    /** 微信绑定状态，可空表示不筛选。 */
    private Boolean wechatBound;
}
