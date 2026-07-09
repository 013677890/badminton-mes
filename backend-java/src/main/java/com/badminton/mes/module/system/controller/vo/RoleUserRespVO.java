package com.badminton.mes.module.system.controller.vo;

import lombok.Data;

/**
 * 按角色反查用户的轻量响应 VO(安灯按角色匹配处理人等场景)。
 *
 * <p>只暴露定位人员所需字段，不含手机号等敏感信息。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@Data
public class RoleUserRespVO {

    /** 用户主键 */
    private Long userId;

    /** 工号 */
    private String userNo;

    /** 姓名 */
    private String userName;

    /** 所属车间 id */
    private Long workshopId;

    /** 所属产线 id */
    private Long lineId;
}
