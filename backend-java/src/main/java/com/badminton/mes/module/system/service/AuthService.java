package com.badminton.mes.module.system.service;

import com.badminton.mes.module.system.controller.vo.AuthLoginReqVO;
import com.badminton.mes.module.system.controller.vo.AuthLoginRespVO;
import com.badminton.mes.module.system.controller.vo.AuthPasswordReqVO;
import com.badminton.mes.module.system.controller.vo.AuthProfileRespVO;
import com.badminton.mes.module.system.controller.vo.AuthRegisterReqVO;

/**
 * 认证 Service 接口：登录、登出、当前用户、修改本人密码。
 *
 * <p>会话与防爆破策略见 wiki/15-认证与权限管理设计.md：
 * Redis 不透明 token、单设备登录、连续失败 5 次锁定 15 分钟。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
public interface AuthService {

    /**
     * 工号 + 密码登录。
     *
     * <p>账户不存在与密码错误统一返回"工号或密码错误"，防撞库探测账号；
     * 账户停用在密码校验通过后才提示。登录成功清空失败计数并写入会话，
     * 同一用户旧会话被互踢(单设备登录)。
     *
     * @param reqVO 登录请求
     * @return token 与用户信息
     * @throws com.badminton.mes.common.exception.ServiceException 凭据错误、
     *         锁定中或账户停用时抛出
     */
    AuthLoginRespVO login(AuthLoginReqVO reqVO);

    /**
     * 注册小程序账号并分配一个允许自助注册的职位。
     *
     * @param reqVO 注册请求
     * @return 新用户主键
     */
    Long register(AuthRegisterReqVO reqVO);

    /**
     * 登出：删除当前请求 token 对应的会话，天然幂等。
     */
    void logout();

    /**
     * 查询当前登录用户信息，实时读库，手机号脱敏(SEC-002)。
     *
     * @return 当前用户信息
     * @throws com.badminton.mes.common.exception.ServiceException 用户已被删除时抛出
     */
    AuthProfileRespVO getProfile();

    /**
     * 修改本人密码：校验旧密码，成功后强制下线，客户端需重新登录。
     *
     * @param reqVO 修改密码请求
     * @throws com.badminton.mes.common.exception.ServiceException 旧密码错误时抛出
     */
    void changePassword(AuthPasswordReqVO reqVO);
}
