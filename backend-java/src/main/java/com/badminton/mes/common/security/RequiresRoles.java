package com.badminton.mes.common.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色访问控制注解，标注在 Controller 方法或类上，
 * 登录用户命中任一声明角色即放行；方法级注解优先于类级。
 *
 * <p>登录控制不需要注解：/api/** 默认全部要求登录，
 * 白名单见 {@code SecurityWebConfig}。角色编码取值见
 * {@code RoleCodeConstants}。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresRoles {

    /**
     * 允许访问的角色编码列表，任一命中即放行。
     *
     * @return 角色编码数组
     */
    String[] value();
}
