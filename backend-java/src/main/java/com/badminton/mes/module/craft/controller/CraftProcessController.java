package com.badminton.mes.module.craft.controller;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.security.RequiresRoles;
import com.badminton.mes.common.security.RoleCodeConstants;
import com.badminton.mes.module.craft.controller.vo.CraftProcessChangeLogPageReqVO;
import com.badminton.mes.module.craft.controller.vo.CraftProcessChangeLogRespVO;
import com.badminton.mes.module.craft.controller.vo.CraftProcessPageReqVO;
import com.badminton.mes.module.craft.controller.vo.CraftProcessRespVO;
import com.badminton.mes.module.craft.controller.vo.CraftProcessSaveReqVO;
import com.badminton.mes.module.craft.controller.vo.CraftProcessStatusReqVO;
import com.badminton.mes.module.craft.controller.vo.CraftProcessUpdateReqVO;
import com.badminton.mes.module.craft.service.CraftProcessService;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * 工序主档 Controller。
 *
 * <p>写操作限管理员和工艺工程师，查询接口登录即可访问。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
@Validated
@RestController
@RequestMapping("/api/craft/processes")
public class CraftProcessController {

    private final CraftProcessService processService;

    /**
     * 构造器注入。
     *
     * @param processService 工序主档 Service
     */
    public CraftProcessController(CraftProcessService processService) {
        this.processService = processService;
    }

    /**
     * 创建工序。
     *
     * @param reqVO 创建请求
     * @return 新工序主键
     */
    @PostMapping
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.CRAFT_ENGINEER})
    public CommonResult<Long> createProcess(@Valid @RequestBody CraftProcessSaveReqVO reqVO) {
        return CommonResult.success(processService.createProcess(reqVO));
    }

    /**
     * 按预期版本修改工序。
     *
     * @param id    工序主键
     * @param reqVO 修改请求
     * @return 空数据成功响应
     */
    @PutMapping("/{id}")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.CRAFT_ENGINEER})
    public CommonResult<Void> updateProcess(@PathVariable("id") @Positive Long id,
                                            @Valid @RequestBody CraftProcessUpdateReqVO reqVO) {
        processService.updateProcess(id, reqVO);
        return CommonResult.success(null);
    }

    /**
     * 按预期版本启用或停用工序。
     *
     * @param id    工序主键
     * @param reqVO 状态变更请求
     * @return 空数据成功响应
     */
    @PutMapping("/{id}/status")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.CRAFT_ENGINEER})
    public CommonResult<Void> updateProcessStatus(@PathVariable("id") @Positive Long id,
                                                  @Valid @RequestBody CraftProcessStatusReqVO reqVO) {
        processService.updateProcessStatus(id, reqVO);
        return CommonResult.success(null);
    }

    /**
     * 按预期版本逻辑删除工序。
     *
     * @param id      工序主键
     * @param version 客户端读取时的版本号
     * @return 空数据成功响应
     */
    @DeleteMapping("/{id}")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.CRAFT_ENGINEER})
    public CommonResult<Void> deleteProcess(
            @PathVariable("id") @Positive Long id,
            @RequestParam("version") @PositiveOrZero Integer version) {
        processService.deleteProcess(id, version);
        return CommonResult.success(null);
    }

    /**
     * 查询工序详情。
     *
     * @param id 工序主键
     * @return 工序详情
     */
    @GetMapping("/{id}")
    public CommonResult<CraftProcessRespVO> getProcess(@PathVariable("id") @Positive Long id) {
        return CommonResult.success(processService.getProcess(id));
    }

    /**
     * 分页查询工序。
     *
     * @param reqVO 分页查询请求
     * @return 工序分页结果
     */
    @GetMapping("/page")
    public CommonResult<PageResult<CraftProcessRespVO>> getProcessPage(
            @Valid CraftProcessPageReqVO reqVO) {
        return CommonResult.success(processService.getProcessPage(reqVO));
    }

    /**
     * 分页查询工序变更日志。
     *
     * @param id    工序主键
     * @param reqVO 分页请求
     * @return 变更日志分页结果
     */
    @GetMapping("/{id}/change_logs")
    public CommonResult<PageResult<CraftProcessChangeLogRespVO>> getProcessChangeLogPage(
            @PathVariable("id") @Positive Long id,
            @Valid CraftProcessChangeLogPageReqVO reqVO) {
        return CommonResult.success(processService.getProcessChangeLogPage(id, reqVO));
    }
}
