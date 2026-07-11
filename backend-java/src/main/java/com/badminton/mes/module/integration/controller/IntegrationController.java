package com.badminton.mes.module.integration.controller;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.security.RequiresRoles;
import com.badminton.mes.common.security.RoleCodeConstants;
import com.badminton.mes.module.integration.controller.vo.ExternalWorkOrderWriteReqVO;
import com.badminton.mes.module.integration.controller.vo.IntegrationWriteLogPageReqVO;
import com.badminton.mes.module.integration.controller.vo.IntegrationWriteLogRespVO;
import com.badminton.mes.module.integration.controller.vo.IntegrationWriteResultRespVO;
import com.badminton.mes.module.integration.controller.vo.UnitWriteReqVO;
import com.badminton.mes.module.integration.service.IntegrationService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * 外部标准写入接口 Controller。
 *
 * <p>当前复用系统登录令牌和角色鉴权；调用账号由管理员分配，所有写入均记录调用用户。
 *
 * @author 张竹灏
 * @date 2026/07/11
 */
@RestController
@RequestMapping("/api/integration")
@RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC})
public class IntegrationController {

    private final IntegrationService integrationService;

    /**
     * 构造外部接口 Controller。
     *
     * @param integrationService 外部接口 Service
     */
    public IntegrationController(IntegrationService integrationService) {
        this.integrationService = integrationService;
    }

    /**
     * 新增或更新计量单位。
     *
     * @param reqVO 单位写入请求
     * @return 写入结果
     */
    @PostMapping("/units")
    public CommonResult<IntegrationWriteResultRespVO> writeUnit(
            @Valid @RequestBody UnitWriteReqVO reqVO) {
        return CommonResult.success(integrationService.writeUnit(reqVO));
    }

    /**
     * 幂等写入外部生产工单。
     *
     * @param reqVO 外部工单写入请求
     * @return 写入结果
     */
    @PostMapping("/work_orders")
    public CommonResult<IntegrationWriteResultRespVO> writeWorkOrder(
            @Valid @RequestBody ExternalWorkOrderWriteReqVO reqVO) {
        return CommonResult.success(integrationService.writeWorkOrder(reqVO));
    }

    /**
     * 分页查询外部接口写入结果。
     *
     * @param reqVO 分页筛选条件
     * @return 写入日志分页
     */
    @GetMapping("/write_logs")
    public CommonResult<PageResult<IntegrationWriteLogRespVO>> getWriteLogPage(
            @Valid @ModelAttribute IntegrationWriteLogPageReqVO reqVO) {
        return CommonResult.success(integrationService.getWriteLogPage(reqVO));
    }
}
