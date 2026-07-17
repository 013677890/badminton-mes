package com.badminton.mes.module.integration.controller;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.security.RequiresRoles;
import com.badminton.mes.common.security.RoleCodeConstants;
import com.badminton.mes.module.integration.controller.vo.ErpCraftSyncReqVO;
import com.badminton.mes.module.integration.controller.vo.ErpCraftSyncRespVO;
import com.badminton.mes.module.integration.controller.vo.ErpCraftPendingPageReqVO;
import com.badminton.mes.module.integration.controller.vo.ErpCraftPendingRespVO;
import com.badminton.mes.module.integration.controller.vo.ErpCraftRejectReqVO;
import com.badminton.mes.module.integration.controller.vo.ErpSyncLogPageReqVO;
import com.badminton.mes.module.integration.controller.vo.ErpTaskSyncReqVO;
import com.badminton.mes.module.integration.controller.vo.ErpTaskSyncRespVO;
import com.badminton.mes.module.integration.controller.vo.IntegrationWriteLogRespVO;
import com.badminton.mes.module.integration.service.ErpSyncService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * ERP 同步接口 Controller，提供生产任务单读取和工艺数据读取端点。
 *
 * <p>任务同步允许 PMC 和管理员触发；工艺确认仅允许工艺工程师和管理员。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@RestController
@RequestMapping("/api/integration/erp")
public class ErpSyncController {

    /** ERP 同步门面，负责批量隔离、结果汇总及工艺待确认状态流转。 */
    private final ErpSyncService erpSyncService;

    /**
     * 构造 ERP 同步 Controller。
     *
     * @param erpSyncService ERP 同步 Service
     */
    public ErpSyncController(ErpSyncService erpSyncService) {
        this.erpSyncService = erpSyncService;
    }

    /**
     * 触发 ERP 生产任务单同步。
     *
     * @param reqVO 同步触发请求
     * @return 同步结果
     */
    @PostMapping("/tasks/sync")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC})
    public CommonResult<ErpTaskSyncRespVO> syncErpTasks(
            @Valid @RequestBody(required = false) ErpTaskSyncReqVO reqVO) {
        // 请求体可省略；空请求表示使用默认来源并同步数据源返回的全部任务。
        ErpTaskSyncReqVO request = reqVO != null ? reqVO : new ErpTaskSyncReqVO();
        return CommonResult.success(erpSyncService.syncErpTasks(request));
    }

    /**
     * 分页查询 ERP 任务同步日志。
     *
     * @param reqVO 分页筛选条件
     * @return 同步日志分页
     */
    @GetMapping("/tasks/sync_logs")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC})
    public CommonResult<PageResult<IntegrationWriteLogRespVO>> getErpTaskSyncLogs(
            @Valid @ModelAttribute ErpSyncLogPageReqVO reqVO) {
        return CommonResult.success(erpSyncService.getErpTaskSyncLogPage(reqVO));
    }

    /**
     * 触发 ERP 工艺数据同步。
     *
     * @param reqVO 同步触发请求
     * @return 同步结果
     */
    @PostMapping("/crafts/sync")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.CRAFT_ENGINEER})
    public CommonResult<ErpCraftSyncRespVO> syncErpCrafts(
            @Valid @RequestBody(required = false) ErpCraftSyncReqVO reqVO) {
        // 空请求同样转为空对象，避免 Service 接收 null 并统一采用默认来源系统。
        ErpCraftSyncReqVO request = reqVO != null ? reqVO : new ErpCraftSyncReqVO();
        return CommonResult.success(erpSyncService.syncErpCrafts(request));
    }

    /**
     * 确认待确认工艺数据，生成 MES 工艺路线草稿。
     *
     * @param id 待确认数据主键
     * @return 新生成的工艺路线主键
     */
    @PutMapping("/crafts/pending/{id}/confirm")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.CRAFT_ENGINEER})
    public CommonResult<Long> confirmPendingCraft(@PathVariable Long id) {
        return CommonResult.success(erpSyncService.confirmPendingCraft(id));
    }

    /** 分页查询待确认、已确认、异常和已驳回工艺数据。 */
    @GetMapping("/crafts/pending")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.CRAFT_ENGINEER})
    public CommonResult<PageResult<ErpCraftPendingRespVO>> getPendingCraftPage(
            @Valid @ModelAttribute ErpCraftPendingPageReqVO reqVO) {
        return CommonResult.success(erpSyncService.getPendingCraftPage(reqVO));
    }

    /** 驳回一条待确认工艺数据。 */
    @PutMapping("/crafts/pending/{id}/reject")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.CRAFT_ENGINEER})
    public CommonResult<Boolean> rejectPendingCraft(
            @PathVariable Long id,
            @Valid @RequestBody ErpCraftRejectReqVO reqVO) {
        erpSyncService.rejectPendingCraft(id, reqVO.getReason());
        return CommonResult.success(true);
    }
}
