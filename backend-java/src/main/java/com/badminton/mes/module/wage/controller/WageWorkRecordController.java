package com.badminton.mes.module.wage.controller;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.security.RequiresRoles;
import com.badminton.mes.common.security.RoleCodeConstants;
import com.badminton.mes.module.wage.controller.vo.WageWorkRecordImportReqVO;
import com.badminton.mes.module.wage.controller.vo.WageWorkRecordImportRespVO;
import com.badminton.mes.module.wage.controller.vo.WageWorkRecordPageReqVO;
import com.badminton.mes.module.wage.controller.vo.WageWorkRecordRespVO;
import com.badminton.mes.module.wage.service.WageWorkRecordService;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/** 已审核报工计件快照 Controller。 */
@Validated
@RestController
@RequestMapping("/api/wage/work_records")
@RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.WORKSHOP_MANAGER})
public class WageWorkRecordController {

    private final WageWorkRecordService workRecordService;

    /** 构造器注入。 */
    public WageWorkRecordController(WageWorkRecordService workRecordService) {
        this.workRecordService = workRecordService;
    }

    /** 幂等导入已审核报工快照。 */
    @PostMapping("/import")
    public CommonResult<WageWorkRecordImportRespVO> importRecords(
            @Valid @RequestBody WageWorkRecordImportReqVO reqVO) {
        return CommonResult.success(workRecordService.importRecords(reqVO));
    }

    /** 分页查询计件报工快照。 */
    @GetMapping("/page")
    public CommonResult<PageResult<WageWorkRecordRespVO>> getRecordPage(
            @Valid WageWorkRecordPageReqVO reqVO) {
        return CommonResult.success(workRecordService.getRecordPage(reqVO));
    }
}
