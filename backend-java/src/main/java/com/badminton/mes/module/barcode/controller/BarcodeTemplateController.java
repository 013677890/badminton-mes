package com.badminton.mes.module.barcode.controller;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.security.RequiresRoles;
import com.badminton.mes.common.security.RoleCodeConstants;
import com.badminton.mes.module.barcode.controller.vo.BarcodeTemplatePageReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeTemplatePreviewReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeTemplatePreviewRespVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeTemplateRespVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeTemplateSaveReqVO;
import com.badminton.mes.module.barcode.service.BarcodeTemplateService;

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
 * 条码模板 Controller。
 *
 * <p>模板不提供删除接口：版本行需长期保留供打印历史追溯，废弃用停用。
 * 第一阶段不对接真实打印机，preview 只返回预览数据(已冻结决策)。
 *
 * <p>接口按角色限权：配置类写操作限管理员与 PMC，预览/查询登录即可。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@RestController
@RequestMapping("/api/barcode/templates")
public class BarcodeTemplateController {

    private final BarcodeTemplateService barcodeTemplateService;

    /**
     * 构造器注入，依赖不可变。
     *
     * @param barcodeTemplateService 条码模板 Service
     */
    public BarcodeTemplateController(BarcodeTemplateService barcodeTemplateService) {
        this.barcodeTemplateService = barcodeTemplateService;
    }

    /**
     * 新增标签模板及字段配置。
     *
     * @param reqVO 创建请求，字段规则见 {@link BarcodeTemplateSaveReqVO}
     * @return 新模板主键 id
     */
    @PostMapping
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC})
    public CommonResult<Long> createBarcodeTemplate(@Valid @RequestBody BarcodeTemplateSaveReqVO reqVO) {
        return CommonResult.success(barcodeTemplateService.createBarcodeTemplate(reqVO));
    }

    /**
     * 修改标签模板：已被应用规则绑定时保留原版本并生成升版本新行。
     *
     * @param id    模板主键
     * @param reqVO 修改请求，模板编码被忽略
     * @return 空数据成功响应
     */
    @PutMapping("/{id}")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC})
    public CommonResult<Void> updateBarcodeTemplate(@PathVariable("id") @Positive Long id,
                                                    @Valid @RequestBody BarcodeTemplateSaveReqVO reqVO) {
        barcodeTemplateService.updateBarcodeTemplate(id, reqVO);
        return CommonResult.success(null);
    }

    /**
     * 启用标签模板。
     *
     * @param id 模板主键
     * @return 空数据成功响应
     */
    @PutMapping("/{id}/enable")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC})
    public CommonResult<Void> enableBarcodeTemplate(@PathVariable("id") @Positive Long id) {
        barcodeTemplateService.enableBarcodeTemplate(id);
        return CommonResult.success(null);
    }

    /**
     * 停用标签模板。
     *
     * @param id 模板主键
     * @return 空数据成功响应
     */
    @PutMapping("/{id}/disable")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC})
    public CommonResult<Void> disableBarcodeTemplate(@PathVariable("id") @Positive Long id) {
        barcodeTemplateService.disableBarcodeTemplate(id);
        return CommonResult.success(null);
    }

    /**
     * 返回模板打印预览数据。
     *
     * @param reqVO 预览请求
     * @return 预览数据：布局与逐字段展示内容
     */
    @PostMapping("/preview")
    public CommonResult<BarcodeTemplatePreviewRespVO> previewBarcodeTemplate(
            @Valid @RequestBody BarcodeTemplatePreviewReqVO reqVO) {
        return CommonResult.success(barcodeTemplateService.previewBarcodeTemplate(reqVO));
    }

    /**
     * 分页查询标签模板。
     *
     * @param reqVO 分页筛选条件，GET 查询参数绑定
     * @return 分页结果，无数据时 list 为空集合(API-002)
     */
    @GetMapping("/page")
    public CommonResult<PageResult<BarcodeTemplateRespVO>> getBarcodeTemplatePage(
            @Valid BarcodeTemplatePageReqVO reqVO) {
        return CommonResult.success(barcodeTemplateService.getBarcodeTemplatePage(reqVO));
    }

    /**
     * 查询模板详情，含字段配置。
     *
     * @param id 模板主键
     * @return 模板详情
     */
    @GetMapping("/{id}")
    public CommonResult<BarcodeTemplateRespVO> getBarcodeTemplate(@PathVariable("id") @Positive Long id) {
        return CommonResult.success(barcodeTemplateService.getBarcodeTemplate(id));
    }
}
