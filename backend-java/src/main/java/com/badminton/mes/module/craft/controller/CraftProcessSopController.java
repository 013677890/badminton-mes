package com.badminton.mes.module.craft.controller;

import java.util.List;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.common.security.RequiresRoles;
import com.badminton.mes.common.security.RoleCodeConstants;
import com.badminton.mes.module.craft.controller.vo.CraftProcessSopRespVO;
import com.badminton.mes.module.craft.controller.vo.CraftProcessSopSaveReqVO;
import com.badminton.mes.module.craft.controller.vo.CraftProcessSopUpdateReqVO;
import com.badminton.mes.module.craft.service.CraftProcessSopService;

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
 * 工序 SOP 关联 Controller。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
@Validated
@RestController
@RequestMapping("/api/craft/processes")
public class CraftProcessSopController {

    private final CraftProcessSopService sopService;

    /**
     * 构造器注入。
     *
     * @param sopService 工序 SOP Service
     */
    public CraftProcessSopController(CraftProcessSopService sopService) {
        this.sopService = sopService;
    }

    /**
     * 为工序新增 SOP。
     *
     * @param id    工序主键
     * @param reqVO 创建请求
     * @return SOP 关联主键
     */
    @PostMapping("/{id}/sops")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.CRAFT_ENGINEER})
    public CommonResult<Long> createProcessSop(@PathVariable("id") @Positive Long id,
                                               @Valid @RequestBody CraftProcessSopSaveReqVO reqVO) {
        return CommonResult.success(sopService.createProcessSop(id, reqVO));
    }

    /**
     * 按预期版本修改工序 SOP。
     *
     * @param id    工序主键
     * @param sopId SOP 关联主键
     * @param reqVO 修改请求
     * @return 空数据成功响应
     */
    @PutMapping("/{id}/sops/{sop_id}")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.CRAFT_ENGINEER})
    public CommonResult<Void> updateProcessSop(
            @PathVariable("id") @Positive Long id,
            @PathVariable("sop_id") @Positive Long sopId,
            @Valid @RequestBody CraftProcessSopUpdateReqVO reqVO) {
        sopService.updateProcessSop(id, sopId, reqVO);
        return CommonResult.success(null);
    }

    /**
     * 按预期版本逻辑删除工序 SOP。
     *
     * @param id      工序主键
     * @param sopId   SOP 关联主键
     * @param version 客户端读取时的版本号
     * @return 空数据成功响应
     */
    @DeleteMapping("/{id}/sops/{sop_id}")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.CRAFT_ENGINEER})
    public CommonResult<Void> deleteProcessSop(
            @PathVariable("id") @Positive Long id,
            @PathVariable("sop_id") @Positive Long sopId,
            @RequestParam("version") @PositiveOrZero Integer version) {
        sopService.deleteProcessSop(id, sopId, version);
        return CommonResult.success(null);
    }

    /**
     * 查询工序 SOP 列表。
     *
     * @param id 工序主键
     * @return SOP 列表
     */
    @GetMapping("/{id}/sops")
    public CommonResult<List<CraftProcessSopRespVO>> getProcessSops(
            @PathVariable("id") @Positive Long id) {
        return CommonResult.success(sopService.getProcessSops(id));
    }
}
