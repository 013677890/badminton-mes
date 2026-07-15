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
 * 设备类别主数据 HTTP 接口。
 *
 * <p>类别用于组织设备台账并形成父子层级。本控制器负责请求参数绑定、Bean Validation 校验、
 * 调用 {@link EquipmentCategoryService} 以及包装统一响应；编码唯一性、父类别有效性、层级成环检查、
 * 删除引用检查等需要访问持久层的规则由 Service 在事务内完成。
 *
 * <p>请求体校验失败和路径主键约束失败均由全局异常处理器转换为统一错误响应，端点不自行捕获或
 * 改写业务异常。
 *
 * @author 角色C
 * @date 2026/07/09
 */
@RestController
@RequestMapping("/api/equipment/categories")
public class EquipmentCategoryController {

    /** 设备类别应用服务，负责类别层级和关联引用的一致性。 */
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
     * 创建设备类别，编码唯一性和父类别可用性由 Service 校验。
     *
     * @param reqVO 已通过字段校验的类别创建数据，规则见 {@link EquipmentCategorySaveReqVO}
     * @return 包含新类别主键的统一成功响应
     */
    @PostMapping
    public CommonResult<Long> createEquipmentCategory(@Valid @RequestBody EquipmentCategorySaveReqVO reqVO) {
        return CommonResult.success(categoryService.createEquipmentCategory(reqVO));
    }

    /**
     * 修改指定设备类别；更换父类别时由 Service 防止自引用或形成循环层级。
     *
     * @param id 类别主键，必须为正数
     * @param reqVO 已通过字段校验的类别更新数据
     * @return 不携带业务数据的统一成功响应
     */
    @PutMapping("/{id}")
    public CommonResult<Void> updateEquipmentCategory(@PathVariable("id") @Positive Long id,
                                                       @Valid @RequestBody EquipmentCategorySaveReqVO reqVO) {
        categoryService.updateEquipmentCategory(id, reqVO);
        return CommonResult.success(null);
    }

    /**
     * 逻辑删除设备类别。
     *
     * <p>存在下级类别或仍被设备台账引用时，Service 将拒绝删除以保持关联数据完整。
     *
     * @param id 类别主键，必须为正数
     * @return 不携带业务数据的统一成功响应
     */
    @DeleteMapping("/{id}")
    public CommonResult<Void> deleteEquipmentCategory(@PathVariable("id") @Positive Long id) {
        categoryService.deleteEquipmentCategory(id);
        return CommonResult.success(null);
    }

    /**
     * 按主键查询未逻辑删除的设备类别详情。
     *
     * @param id 类别主键，必须为正数
     * @return 设备类别详情统一响应
     */
    @GetMapping("/{id}")
    public CommonResult<EquipmentCategoryRespVO> getEquipmentCategory(@PathVariable("id") @Positive Long id) {
        return CommonResult.success(categoryService.getEquipmentCategory(id));
    }

    /**
     * 按关键字、父类别和启停状态组合分页查询设备类别。
     *
     * @param reqVO 由 GET 查询参数绑定形成的分页筛选条件
     * @return 设备类别分页结果，无匹配数据时列表为空集合
     */
    @GetMapping("/page")
    public CommonResult<PageResult<EquipmentCategoryRespVO>> getEquipmentCategoryPage(
            @Valid EquipmentCategoryPageReqVO reqVO) {
        return CommonResult.success(categoryService.getEquipmentCategoryPage(reqVO));
    }
}
