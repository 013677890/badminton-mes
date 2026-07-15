package com.badminton.mes.module.barcode.controller;

import java.util.List;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.security.RequiresRoles;
import com.badminton.mes.common.security.RoleCodeConstants;
import com.badminton.mes.module.barcode.controller.vo.BarcodeBatchGenerateReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeCancelReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeGenerateReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeGenerateRespVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeImportReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeImportRespVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeInstancePageReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeInstanceRespVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeParseReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeParseRespVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodePrintReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodePrintRespVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeUseRecordRespVO;
import com.badminton.mes.module.barcode.service.BarcodeInstanceService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

/**
 * 条码实例 Controller：生成、解析、作废与查询。
 *
 * <p>接口按角色限权(02-条码应用需求分析参与角色)：生成与作废限管理员
 * 和 PMC 计划员；解析供现场扫码识别，登录即可。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@RestController
@RequestMapping("/api/barcode/instances")
public class BarcodeInstanceController {

    private final BarcodeInstanceService barcodeInstanceService;

    /**
     * 构造器注入，依赖不可变。
     *
     * @param barcodeInstanceService 条码实例 Service
     */
    public BarcodeInstanceController(BarcodeInstanceService barcodeInstanceService) {
        this.barcodeInstanceService = barcodeInstanceService;
    }

    /**
     * 生成批次码。
     *
     * @param reqVO 生成请求，字段规则见 {@link BarcodeGenerateReqVO}
     * @return 生成结果
     */
    @PostMapping("/generate")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC})
    public CommonResult<BarcodeGenerateRespVO> generateBarcode(
            @Valid @RequestBody BarcodeGenerateReqVO reqVO) {
        return CommonResult.success(barcodeInstanceService.generateBarcode(reqVO));
    }

    /**
     * 批量生成批次码，单次上限 500。
     *
     * @param reqVO 批量生成请求
     * @return 生成结果列表
     */
    @PostMapping("/batch_generate")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC})
    public CommonResult<List<BarcodeGenerateRespVO>> batchGenerateBarcodes(
            @Valid @RequestBody BarcodeBatchGenerateReqVO reqVO) {
        return CommonResult.success(barcodeInstanceService.batchGenerateBarcodes(reqVO));
    }

    /**
     * 导入外部批次码：JSON 数组同步导入，单次上限 500，部分成功并返回
     * 逐条失败原因(M1 待确认事项②口径)。
     *
     * @param reqVO 导入请求
     * @return 导入结果
     */
    @PostMapping("/import")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC})
    public CommonResult<BarcodeImportRespVO> importBarcodes(
            @Valid @RequestBody BarcodeImportReqVO reqVO) {
        return CommonResult.success(barcodeInstanceService.importBarcodes(reqVO));
    }

    /**
     * 解析条码值，返回条码事实与业务对象上下文。
     *
     * @param reqVO 解析请求
     * @return 解析结果
     */
    @PostMapping("/parse")
    public CommonResult<BarcodeParseRespVO> parseBarcode(@Valid @RequestBody BarcodeParseReqVO reqVO) {
        return CommonResult.success(barcodeInstanceService.parseBarcode(reqVO));
    }

    /**
     * 记录打印动作并返回预览数据，不驱动真实打印机(已冻结决策)。
     * 打印分发由班组长执行，管理员与 PMC 亦可操作。
     *
     * @param id    条码主键
     * @param reqVO 打印请求，重复打印必须填写原因
     * @return 打印记录事实与预览数据
     */
    @PostMapping("/{id}/print")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC, RoleCodeConstants.TEAM_LEADER})
    public CommonResult<BarcodePrintRespVO> printBarcode(@PathVariable("id") @Positive Long id,
                                                         @Valid @RequestBody BarcodePrintReqVO reqVO) {
        return CommonResult.success(barcodeInstanceService.printBarcode(id, reqVO));
    }

    /**
     * 作废未使用条码；已使用条码不能作废。
     *
     * @param id    条码主键
     * @param reqVO 作废请求，原因仅日志留痕
     * @return 空数据成功响应
     */
    @PutMapping("/{id}/cancel")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC})
    public CommonResult<Void> cancelBarcode(@PathVariable("id") @Positive Long id,
                                            @Valid @RequestBody BarcodeCancelReqVO reqVO) {
        barcodeInstanceService.cancelBarcode(id, reqVO);
        return CommonResult.success(null);
    }

    /**
     * 分页查询条码实例。
     *
     * @param reqVO 分页筛选条件，GET 查询参数绑定
     * @return 分页结果，无数据时 list 为空集合(API-002)
     */
    @GetMapping("/page")
    public CommonResult<PageResult<BarcodeInstanceRespVO>> getBarcodeInstancePage(
            @Valid BarcodeInstancePageReqVO reqVO) {
        return CommonResult.success(barcodeInstanceService.getBarcodeInstancePage(reqVO));
    }

    /**
     * 查询条码扫码使用记录，按业务发生时间倒序。
     *
     * @param id 条码主键
     * @return 使用记录列表，无数据时为空集合(API-002)
     */
    @GetMapping("/{id}/use_records")
    public CommonResult<List<BarcodeUseRecordRespVO>> getBarcodeUseRecords(
            @PathVariable("id") @Positive Long id) {
        return CommonResult.success(barcodeInstanceService.getBarcodeUseRecords(id));
    }

    /**
     * 查询条码详情。
     *
     * @param id 条码主键
     * @return 条码详情
     */
    @GetMapping("/{id}")
    public CommonResult<BarcodeInstanceRespVO> getBarcodeInstance(@PathVariable("id") @Positive Long id) {
        return CommonResult.success(barcodeInstanceService.getBarcodeInstance(id));
    }
}
