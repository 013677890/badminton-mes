package com.badminton.mes.module.system.controller.vo;

import lombok.Data;

/**
 * 角色响应 VO，用户表单下拉与角色展示使用。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@Data
public class RoleRespVO {

    /** 角色主键 */
    private Long id;

    /** 角色编码 */
    private String roleCode;

    /** 角色名称 */
    private String roleName;

    /** 备注 */
    private String remark;

    /** 状态：1 启用 0 停用 */
    private Integer status;
}
