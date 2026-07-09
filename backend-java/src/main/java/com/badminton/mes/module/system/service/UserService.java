package com.badminton.mes.module.system.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.system.controller.vo.UserPageReqVO;
import com.badminton.mes.module.system.controller.vo.UserRespVO;
import com.badminton.mes.module.system.controller.vo.UserSaveReqVO;

/**
 * 系统用户 Service 接口，仅管理员可用(Controller 层 @RequiresRoles 控制)。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
public interface UserService {

    /**
     * 新增用户：工号唯一校验、初始密码必填(BCrypt 落库)、分配角色。
     *
     * @param reqVO 创建请求
     * @return 新用户主键 id
     * @throws com.badminton.mes.common.exception.ServiceException 工号重复、
     *         缺少初始密码或角色不可用时抛出
     */
    Long createUser(UserSaveReqVO reqVO);

    /**
     * 修改用户姓名/手机/车间/产线/角色；工号与密码不在此修改。
     *
     * <p>角色发生变化时强制该用户下线，重新登录后新角色生效，
     * 避免降权后旧会话继续持有原角色。
     *
     * @param id    用户主键
     * @param reqVO 修改请求，userNo 与 password 字段被忽略
     * @throws com.badminton.mes.common.exception.ServiceException 用户不存在
     *         或角色不可用时抛出
     */
    void updateUser(Long id, UserSaveReqVO reqVO);

    /**
     * 逻辑删除用户并强制下线，授权关系随用户失效；不能删除当前登录账号。
     *
     * @param id 用户主键
     * @throws com.badminton.mes.common.exception.ServiceException 用户不存在
     *         或删除自己时抛出
     */
    void deleteUser(Long id);

    /**
     * 启用/停用用户，停用即强制下线；不能停用当前登录账号。
     *
     * @param id     用户主键
     * @param status 目标状态(1 启用 0 停用)
     * @throws com.badminton.mes.common.exception.ServiceException 用户不存在
     *         或停用自己时抛出
     */
    void updateUserStatus(Long id, Integer status);

    /**
     * 管理员重置用户密码，重置后强制下线。
     *
     * @param id          用户主键
     * @param newPassword 新密码明文，BCrypt 后落库
     * @throws com.badminton.mes.common.exception.ServiceException 用户不存在时抛出
     */
    void resetPassword(Long id, String newPassword);

    /**
     * 查询用户详情(含角色)，手机号脱敏。
     *
     * @param id 用户主键
     * @return 用户详情
     * @throws com.badminton.mes.common.exception.ServiceException 用户不存在时抛出
     */
    UserRespVO getUser(Long id);

    /**
     * 分页查询用户，按工号/姓名/车间/角色/状态筛选，手机号脱敏。
     *
     * <p>先 count 后查列表，总数为 0 时直接返回空页；
     * 请求页码超过总页数时按最后一页返回(API-009)。
     *
     * @param reqVO 分页筛选条件
     * @return 分页结果，无数据时 list 为空集合而非 null(API-002)
     */
    PageResult<UserRespVO> getUserPage(UserPageReqVO reqVO);
}
