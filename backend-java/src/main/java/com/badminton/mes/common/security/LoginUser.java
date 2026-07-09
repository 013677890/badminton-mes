package com.badminton.mes.common.security;

import java.util.List;

import lombok.Data;

/**
 * 登录用户会话载荷，登录时写入 Redis，拦截器命中后注入
 * {@link SecurityContextHolder} 供各模块读取当前操作人。
 *
 * <p>只承载鉴权所需最小字段，不含密码等敏感信息；管理员调整用户角色后
 * 会话不回写，需重新登录生效。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@Data
public class LoginUser {

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

    /** 角色编码列表，@RequiresRoles 校验依据 */
    private List<String> roleCodes;
}
