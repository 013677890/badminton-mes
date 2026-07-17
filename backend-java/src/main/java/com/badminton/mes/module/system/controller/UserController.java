package com.badminton.mes.module.system.controller;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.security.RequiresRoles;
import com.badminton.mes.common.security.RoleCodeConstants;
import com.badminton.mes.module.system.controller.vo.UserPageReqVO;
import com.badminton.mes.module.system.controller.vo.UserPasswordResetReqVO;
import com.badminton.mes.module.system.controller.vo.UserAssignmentReqVO;
import com.badminton.mes.module.system.controller.vo.UserRespVO;
import com.badminton.mes.module.system.controller.vo.UserSaveReqVO;
import com.badminton.mes.module.system.controller.vo.UserStatusReqVO;
import com.badminton.mes.module.system.service.UserService;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

/**
 * 系统用户管理 Controller。
 *
 * <p>Web 层职责保持单薄：声明式参数校验、转发 Service、包装统一响应。
 * 用户查询和小程序职位分配对所有登录用户开放；账号管理操作仍由
 * {@link RequiresRoles} 限制为管理员，ADMIN 职位的授予与撤销在 Service 层二次校验。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@RestController
@RequestMapping("/api/system/users")
public class UserController {

    private final UserService userService;

    /**
     * 构造器注入，依赖不可变。
     *
     * @param userService 用户 Service
     */
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 新增用户。
     *
     * @param reqVO 创建请求，初始密码必填
     * @return 新用户主键 id
     */
    @PostMapping
    @RequiresRoles(RoleCodeConstants.ADMIN)
    public CommonResult<Long> createUser(@Valid @RequestBody UserSaveReqVO reqVO) {
        return CommonResult.success(userService.createUser(reqVO));
    }

    /**
     * 修改用户基础信息与角色。请求体中的 userNo 与 password 字段在修改时
     * 被忽略：工号不允许变更，密码变更走 {@link #resetPassword} 专用接口。
     *
     * @param id    用户主键
     * @param reqVO 修改请求，userNo 与 password 字段被忽略
     * @return 空数据成功响应
     */
    @PutMapping("/{id}")
    @RequiresRoles(RoleCodeConstants.ADMIN)
    public CommonResult<Void> updateUser(@PathVariable("id") @Positive Long id,
                                         @Valid @RequestBody UserSaveReqVO reqVO) {
        userService.updateUser(id, reqVO);
        return CommonResult.success(null);
    }

    /**
     * 调整用户职位、车间和产线。
     *
     * <p>所有登录用户都可调整普通职位和所属组织；只有当前管理员可以授予或撤销
     * ADMIN。修改成功后目标用户全部会话失效，重新登录后新权限生效。
     *
     * @param id 用户主键
     * @param reqVO 职位与组织分配请求
     * @return 空数据成功响应
     */
    @PutMapping("/{id}/assignment")
    public CommonResult<Void> updateUserAssignment(
            @PathVariable("id") @Positive Long id,
            @Valid @RequestBody UserAssignmentReqVO reqVO) {
        userService.updateUserAssignment(id, reqVO);
        return CommonResult.success(null);
    }

    /**
     * 删除用户(逻辑删除)，不能删除当前登录账号。
     *
     * @param id 用户主键
     * @return 空数据成功响应
     */
    @DeleteMapping("/{id}")
    @RequiresRoles(RoleCodeConstants.ADMIN)
    public CommonResult<Void> deleteUser(@PathVariable("id") @Positive Long id) {
        userService.deleteUser(id);
        return CommonResult.success(null);
    }

    /**
     * 启用/停用用户，停用即强制下线。
     *
     * @param id    用户主键
     * @param reqVO 目标状态
     * @return 空数据成功响应
     */
    @PutMapping("/{id}/status")
    @RequiresRoles(RoleCodeConstants.ADMIN)
    public CommonResult<Void> updateUserStatus(@PathVariable("id") @Positive Long id,
                                               @Valid @RequestBody UserStatusReqVO reqVO) {
        userService.updateUserStatus(id, reqVO.getStatus());
        return CommonResult.success(null);
    }

    /**
     * 重置用户密码，重置后该用户强制下线。
     *
     * @param id    用户主键
     * @param reqVO 新密码
     * @return 空数据成功响应
     */
    @PutMapping("/{id}/password/reset")
    @RequiresRoles(RoleCodeConstants.ADMIN)
    public CommonResult<Void> resetPassword(@PathVariable("id") @Positive Long id,
                                            @Valid @RequestBody UserPasswordResetReqVO reqVO) {
        userService.resetPassword(id, reqVO.getNewPassword());
        return CommonResult.success(null);
    }

    /**
     * 查询用户详情(含角色)。
     *
     * @param id 用户主键
     * @return 用户详情，手机号脱敏
     */
    @GetMapping("/{id}")
    public CommonResult<UserRespVO> getUser(@PathVariable("id") @Positive Long id) {
        return CommonResult.success(userService.getUser(id));
    }

    /**
     * 分页查询用户列表。
     *
     * @param reqVO 分页筛选条件，GET 查询参数绑定
     * @return 分页结果，无数据时 list 为空集合(API-002)
     */
    @GetMapping("/page")
    public CommonResult<PageResult<UserRespVO>> getUserPage(@Valid UserPageReqVO reqVO) {
        return CommonResult.success(userService.getUserPage(reqVO));
    }
}
