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
 * 质量检验单统一作业接口。
 *
 * <p>统一承载首件（FIRST_ARTICLE）、末件（LAST_ARTICLE）、巡检（PATROL）、入库检（WAREHOUSE_IN）
 * 和发货检（SHIPMENT）五类检验；检验员维护草稿结果并提交结论，其他生产管理岗位仅查询检验记录。
 */
@RestController
@RequestMapping("/api/quality/inspection-records")
public class QualityInspectionRecordController {

    private final QualityInspectionRecordService recordService;

    public QualityInspectionRecordController(QualityInspectionRecordService recordService) {
        this.recordService = recordService;
    }

    /**
     * 按指定检验类型创建草稿检验单并返回主键。
     *
     * <p>{@code inspectionType} 仅接受五种受支持的英文枚举值，且必须与所选生效方案的检验类型一致。
     */
    @PostMapping
    @RequiresRoles({ADMIN, INSPECTOR})
    public CommonResult<Long> create(
            @RequestParam
            @Pattern(regexp = "^(FIRST_ARTICLE|LAST_ARTICLE|PATROL|WAREHOUSE_IN|SHIPMENT)$",
                     message = "检验类型不合法") String inspectionType,
            @Valid @RequestBody QualityInspectionRecordCreateReqVO request) {
        return CommonResult.success(recordService.createRecord(inspectionType, request));
    }

    /** 批量保存指定草稿检验单的项目实测值和逐项判定，检验单主键必须为正整数。 */
    @PutMapping("/{id}/results")
    @RequiresRoles({ADMIN, INSPECTOR})
    public CommonResult<Void> saveResults(@PathVariable @Positive Long id,
                                          @Valid @RequestBody QualityInspectionResultsSaveReqVO request) {
        recordService.saveResults(id, request);
        return CommonResult.success(null);
    }

    /** 提交指定草稿检验单的最终结论；提交后记录进入 SUBMITTED 状态，不再作为草稿编辑。 */
    @PutMapping("/{id}/submit")
    @RequiresRoles({ADMIN, INSPECTOR})
    public CommonResult<Void> submit(@PathVariable @Positive Long id,
                                     @Valid @RequestBody QualityInspectionRecordSubmitReqVO request) {
        recordService.submitRecord(id, request);
        return CommonResult.success(null);
    }

    /** 按正整数主键查询检验单、方案快照信息及全部项目结果。 */
    @GetMapping("/{id}")
    @RequiresRoles({ADMIN, INSPECTOR, PMC, WORKSHOP_MANAGER, TEAM_LEADER})
    public CommonResult<QualityInspectionRecordRespVO> get(@PathVariable @Positive Long id) {
        return CommonResult.success(recordService.getRecord(id));
    }

    /** 按检验类型、单据状态、结论及生产来源条件分页筛选检验单。 */
    @GetMapping("/page")
    @RequiresRoles({ADMIN, INSPECTOR, PMC, WORKSHOP_MANAGER, TEAM_LEADER})
    public CommonResult<PageResult<QualityInspectionRecordRespVO>> page(
            @Valid QualityInspectionRecordPageReqVO request) {
        return CommonResult.success(recordService.getRecordPage(request));
    }
}
