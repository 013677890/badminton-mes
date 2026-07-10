package com.badminton.mes.module.craft.controller;

import java.util.List;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.common.security.RequiresRoles;
import com.badminton.mes.common.security.RoleCodeConstants;
import com.badminton.mes.module.craft.controller.vo.CraftProcessDefectReasonRespVO;
import com.badminton.mes.module.craft.controller.vo.CraftProcessDefectReasonSaveReqVO;
import com.badminton.mes.module.craft.controller.vo.CraftProcessDefectReasonUpdateReqVO;
import com.badminton.mes.module.craft.service.CraftProcessDefectReasonService;

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
 * 工序不良原因关联 Controller。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
@Validated
@RestController
@RequestMapping("/api/craft/processes")
public class CraftProcessDefectReasonController {

    private final CraftProcessDefectReasonService defectReasonService;

    /**
     * 构造器注入。
     *
     * @param defectReasonService 工序不良原因 Service
     */
    public CraftProcessDefectReasonController(CraftProcessDefectReasonService defectReasonService) {
        this.defectReasonService = defectReasonService;
    }

    /**
     * 为工序新增不良原因。
     *
     * @param id    工序主键
     * @param reqVO 创建请求
     * @return 不良原因关联主键
     */
    @PostMapping("/{id}/defect_reasons")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.CRAFT_ENGINEER})
    public CommonResult<Long> createProcessDefectReason(
            @PathVariable("id") @Positive Long id,
            @Valid @RequestBody CraftProcessDefectReasonSaveReqVO reqVO) {
        return CommonResult.success(defectReasonService.createProcessDefectReason(id, reqVO));
    }

    /**
     * 按预期版本修改工序不良原因。
     *
     * @param id       工序主键
     * @param reasonId 不良原因关联主键
     * @param reqVO    修改请求
     * @return 空数据成功响应
     */
    @PutMapping("/{id}/defect_reasons/{reason_id}")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.CRAFT_ENGINEER})
    public CommonResult<Void> updateProcessDefectReason(
            @PathVariable("id") @Positive Long id,
            @PathVariable("reason_id") @Positive Long reasonId,
            @Valid @RequestBody CraftProcessDefectReasonUpdateReqVO reqVO) {
        defectReasonService.updateProcessDefectReason(id, reasonId, reqVO);
        return CommonResult.success(null);
    }

    /**
     * 按预期版本逻辑删除工序不良原因。
     *
     * @param id       工序主键
     * @param reasonId 不良原因关联主键
     * @param version  客户端读取时的版本号
     * @return 空数据成功响应
     */
    @DeleteMapping("/{id}/defect_reasons/{reason_id}")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.CRAFT_ENGINEER})
    public CommonResult<Void> deleteProcessDefectReason(
            @PathVariable("id") @Positive Long id,
            @PathVariable("reason_id") @Positive Long reasonId,
            @RequestParam("version") @PositiveOrZero Integer version) {
        defectReasonService.deleteProcessDefectReason(id, reasonId, version);
        return CommonResult.success(null);
    }

    /**
     * 查询工序不良原因列表。
     *
     * @param id 工序主键
     * @return 不良原因列表
     */
    @GetMapping("/{id}/defect_reasons")
    public CommonResult<List<CraftProcessDefectReasonRespVO>> getProcessDefectReasons(
            @PathVariable("id") @Positive Long id) {
        return CommonResult.success(defectReasonService.getProcessDefectReasons(id));
    }
}
