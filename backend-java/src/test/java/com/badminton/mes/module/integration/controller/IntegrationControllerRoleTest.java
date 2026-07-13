package com.badminton.mes.module.integration.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.badminton.mes.common.core.GlobalErrorCodeConstants;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.AuthInterceptor;
import com.badminton.mes.common.security.LoginSessionReader;
import com.badminton.mes.common.security.LoginUser;
import com.badminton.mes.common.security.RoleCodeConstants;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.integration.service.CompletionOrderReadService;
import com.badminton.mes.module.integration.service.DeviceCountWriteCommandService;
import com.badminton.mes.module.integration.service.IntegrationService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.web.method.HandlerMethod;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * {@link IntegrationController} 与真实鉴权拦截器的角色矩阵测试。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@ExtendWith(MockitoExtension.class)
class IntegrationControllerRoleTest {

    private static final String TOKEN = "integration-token";

    private static final List<String> ENDPOINT_METHODS = List.of(
            "writeDeviceCount",
            "getDeviceCountExceptionPage",
            "getCompletionOrderPage",
            "getCompletionReadLogPage");

    @Mock
    private IntegrationService integrationService;

    @Mock
    private DeviceCountWriteCommandService deviceCountWriteCommandService;

    @Mock
    private CompletionOrderReadService completionOrderReadService;

    @Mock
    private LoginSessionReader loginSessionReader;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private IntegrationController controller;

    private AuthInterceptor authInterceptor;

    @BeforeEach
    void setUp() {
        controller = new IntegrationController(
                integrationService, deviceCountWriteCommandService, completionOrderReadService);
        authInterceptor = new AuthInterceptor(loginSessionReader);
        when(request.getHeader(HttpHeaders.AUTHORIZATION))
                .thenReturn(AuthInterceptor.BEARER_PREFIX + TOKEN);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
    }

    @Test
    @DisplayName("集成接口：管理员和 PMC 可访问新增的四个端点")
    void integrationEndpointsAllowAdminAndPmc() {
        for (String role : List.of(RoleCodeConstants.ADMIN, RoleCodeConstants.PMC)) {
            when(loginSessionReader.resolve(TOKEN))
                    .thenReturn(Optional.of(buildLoginUser(role)));
            for (String methodName : ENDPOINT_METHODS) {
                assertThat(authInterceptor.preHandle(
                        request, response, handlerMethod(methodName))).isTrue();
                SecurityContextHolder.clear();
            }
        }
    }

    @Test
    @DisplayName("集成接口：普通操作员访问新增端点均被拒绝")
    void integrationEndpointsRejectOperator() {
        when(loginSessionReader.resolve(TOKEN))
                .thenReturn(Optional.of(buildLoginUser(RoleCodeConstants.OPERATOR)));

        for (String methodName : ENDPOINT_METHODS) {
            assertThatThrownBy(() -> authInterceptor.preHandle(
                    request, response, handlerMethod(methodName)))
                    .isInstanceOfSatisfying(ServiceException.class, exception ->
                            assertThat(exception.getErrorCode())
                                    .isEqualTo(GlobalErrorCodeConstants.FORBIDDEN));
            assertThat(SecurityContextHolder.getLoginUser()).isNull();
        }
    }

    private HandlerMethod handlerMethod(String methodName) {
        return Arrays.stream(IntegrationController.class.getMethods())
                .filter(method -> method.getName().equals(methodName))
                .findFirst()
                .map(method -> new HandlerMethod(controller, method))
                .orElseThrow(() -> new IllegalStateException(
                        "集成接口方法不存在: " + methodName));
    }

    private LoginUser buildLoginUser(String role) {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(1L);
        loginUser.setRoleCodes(List.of(role));
        return loginUser;
    }
}
