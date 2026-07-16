package com.badminton.mes.module.quality.controller;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.security.RequiresRoles;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionRecordCreateReqVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionRecordPageReqVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionRecordRespVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionRecordSubmitReqVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionResultsSaveReqVO;
import com.badminton.mes.module.quality.service.QualityInspectionRecordService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import static com.badminton.mes.common.security.RoleCodeConstants.ADMIN;
import static com.badminton.mes.common.security.RoleCodeConstants.INSPECTOR;
import static com.badminton.mes.common.security.RoleCodeConstants.PMC;
import static com.badminton.mes.common.security.RoleCodeConstants.TEAM_LEADER;
import static com.badminton.mes.common.security.RoleCodeConstants.WORKSHOP_MANAGER;

/**
 * 首末件、巡检、入库检和发货检统一 REST 接口。
 *
 * <p>创建接口通过 {@code inspectionType} 区分检验场景；结果保存和提交分成两个请求，
 * 由 Service 在提交时做必检项完整性和放行状态计算。
 *
 * @author MES 开发组
 * @date 2026/07/16
 */
@RestController
@RequestMapping("/api/quality/inspection-records")
public class QualityInspectionRecordController {

    private final QualityInspectionRecordService recordService;

    public QualityInspectionRecordController(QualityInspectionRecordService recordService) {
        this.recordService = recordService;
    }

    /** 创建指定检验类型的检验单草稿。 */
    @PostMapping
    @RequiresRoles({ADMIN, INSPECTOR})
    public CommonResult<Long> create(
            @RequestParam
            @Pattern(regexp = "^(FIRST_ARTICLE|LAST_ARTICLE|PATROL|WAREHOUSE_IN|SHIPMENT)$",
                     message = "检验类型不合法") String inspectionType,
            @Valid @RequestBody QualityInspectionRecordCreateReqVO request) {
        return CommonResult.success(recordService.createRecord(inspectionType, request));
    }

    /** 保存检验项目结果，检验单仍保持草稿状态。 */
    @PutMapping("/{id}/results")
    @RequiresRoles({ADMIN, INSPECTOR})
    public CommonResult<Void> saveResults(@PathVariable @Positive Long id,
                                          @Valid @RequestBody QualityInspectionResultsSaveReqVO request) {
        recordService.saveResults(id, request);
        return CommonResult.success(null);
    }

    /** 提交检验单并计算最终质量结论。 */
    @PutMapping("/{id}/submit")
    @RequiresRoles({ADMIN, INSPECTOR})
    public CommonResult<Void> submit(@PathVariable @Positive Long id,
                                     @Valid @RequestBody QualityInspectionRecordSubmitReqVO request) {
        recordService.submitRecord(id, request);
        return CommonResult.success(null);
    }

    /** 查询检验单详情和结果明细。 */
    @GetMapping("/{id}")
    @RequiresRoles({ADMIN, INSPECTOR, PMC, WORKSHOP_MANAGER, TEAM_LEADER})
    public CommonResult<QualityInspectionRecordRespVO> get(@PathVariable @Positive Long id) {
        return CommonResult.success(recordService.getRecord(id));
    }

    /** 分页查询检验单。 */
    @GetMapping("/page")
    @RequiresRoles({ADMIN, INSPECTOR, PMC, WORKSHOP_MANAGER, TEAM_LEADER})
    public CommonResult<PageResult<QualityInspectionRecordRespVO>> page(
            @Valid QualityInspectionRecordPageReqVO request) {
        return CommonResult.success(recordService.getRecordPage(request));
    }
}
