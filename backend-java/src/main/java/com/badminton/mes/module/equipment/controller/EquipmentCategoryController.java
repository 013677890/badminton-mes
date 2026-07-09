package com.badminton.mes.module.equipment.controller;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.equipment.controller.vo.EquipmentCategoryPageReqVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentCategoryRespVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentCategorySaveReqVO;
import com.badminton.mes.module.equipment.service.EquipmentCategoryService;

import org.springframework.web.bind.annotation.DeleteMapping;
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
 * 设备类别 Controller。
 *
 * <p>Web 层职责保持单薄：声明式参数校验、转发 Service、包装统一响应，
 * 不写业务规则。路径全小写、单词用下划线分隔、资源名词用复数。
 *
 * <p>校验失败由 {@code GlobalExceptionHandler} 统一转为 A0400 响应；
 * 请求体字段校验触发 MethodArgumentNotValidException，路径参数约束触发
 * HandlerMethodValidationException。
 *
 * @author 角色C
 * @date 2026/07/09
 */
@RestController
@RequestMapping("/api/equipment/categories")
public class EquipmentCategoryController {

    private final EquipmentCategoryService categoryService;

    /**
     * 构造器注入，依赖不可变。
     *
     * @param categoryService 设备类别 Service
     */
    public EquipmentCategoryController(EquipmentCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * 创建设备类别。
     *
     * @param reqVO 创建请求，字段规则见 {@link EquipmentCategorySaveReqVO}
     * @return 新类别主键 id
     */
    @PostMapping
    public CommonResult<Long> createEquipmentCategory(@Valid @RequestBody EquipmentCategorySaveReqVO reqVO) {
        return CommonResult.success(categoryService.createEquipmentCategory(reqVO));
    }

    /**
     * 修改设备类别。
     *
     * @param id    类别主键
     * @param reqVO 修改请求
     * @return 空数据成功响应
     */
    @PutMapping("/{id}")
    public CommonResult<Void> updateEquipmentCategory(@PathVariable("id") @Positive Long id,
                                                       @Valid @RequestBody EquipmentCategorySaveReqVO reqVO) {
        categoryService.updateEquipmentCategory(id, reqVO);
        return CommonResult.success(null);
    }

    /**
     * 删除设备类别(逻辑删除)。
     *
     * <p>存在下级分类或该类别下存在设备时不允许删除。
     *
     * @param id 类别主键
     * @return 空数据成功响应
     */
    @DeleteMapping("/{id}")
    public CommonResult<Void> deleteEquipmentCategory(@PathVariable("id") @Positive Long id) {
        categoryService.deleteEquipmentCategory(id);
        return CommonResult.success(null);
    }

    /**
     * 查询设备类别详情。
     *
     * @param id 类别主键
     * @return 类别详情
     */
    @GetMapping("/{id}")
    public CommonResult<EquipmentCategoryRespVO> getEquipmentCategory(@PathVariable("id") @Positive Long id) {
        return CommonResult.success(categoryService.getEquipmentCategory(id));
    }

    /**
     * 分页查询设备类别列表。
     *
     * @param reqVO 分页筛选条件，GET 查询参数绑定
     * @return 分页结果，无数据时 list 为空集合
     */
    @GetMapping("/page")
    public CommonResult<PageResult<EquipmentCategoryRespVO>> getEquipmentCategoryPage(
            @Valid EquipmentCategoryPageReqVO reqVO) {
        return CommonResult.success(categoryService.getEquipmentCategoryPage(reqVO));
    }
}
