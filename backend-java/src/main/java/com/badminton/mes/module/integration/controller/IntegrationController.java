package com.badminton.mes.module.integration.controller;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.security.RequiresRoles;
import com.badminton.mes.common.security.RoleCodeConstants;
import com.badminton.mes.module.integration.controller.vo.ExternalWorkOrderWriteReqVO;
import com.badminton.mes.module.integration.controller.vo.ExternalDispatchOrderWriteReqVO;
import com.badminton.mes.module.integration.controller.vo.CompletionOrderPageReqVO;
import com.badminton.mes.module.integration.controller.vo.CompletionOrderRespVO;
import com.badminton.mes.module.integration.controller.vo.CompletionReadLogPageReqVO;
import com.badminton.mes.module.integration.controller.vo.CompletionReadLogRespVO;
import com.badminton.mes.module.integration.controller.vo.DeviceCountExceptionPageReqVO;
import com.badminton.mes.module.integration.controller.vo.DeviceCountExceptionRespVO;
import com.badminton.mes.module.integration.controller.vo.DeviceCountWriteReqVO;
import com.badminton.mes.module.integration.controller.vo.DeviceExceptionActionReqVO;
import com.badminton.mes.module.integration.controller.vo.EquipmentBindingSaveReqVO;
import com.badminton.mes.module.integration.controller.vo.MaterialStockBatchReqVO;
import com.badminton.mes.module.integration.controller.vo.IntegrationWriteLogPageReqVO;
import com.badminton.mes.module.integration.controller.vo.IntegrationWriteLogRespVO;
import com.badminton.mes.module.integration.controller.vo.IntegrationWriteResultRespVO;
import com.badminton.mes.module.integration.controller.vo.UnitWriteReqVO;
import com.badminton.mes.module.integration.service.IntegrationService;
import com.badminton.mes.module.integration.service.CompletionOrderReadService;
import com.badminton.mes.module.integration.service.DeviceCountWriteCommandService;
import com.badminton.mes.module.integration.service.EquipmentBindingService;
import com.badminton.mes.module.integration.service.MaterialStockSyncService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;

import jakarta.validation.Valid;

/**
 * 外部标准写入接口 Controller。
 *
 * <p>当前复用系统登录令牌和角色鉴权；调用账号由管理员分配，所有写入均记录调用用户。
 *
 * @author 张竹灏
 * @date 2026/07/11
 */
@RestController
@RequestMapping("/api/integration")
@RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC})
public class IntegrationController {

    private final IntegrationService integrationService;

    private final DeviceCountWriteCommandService deviceCountWriteCommandService;

    private final CompletionOrderReadService completionOrderReadService;

    private final EquipmentBindingService equipmentBindingService;

    private final MaterialStockSyncService materialStockSyncService;

    /**
     * 构造外部接口 Controller。
     *
     * @param integrationService 外部接口 Service
     * @param deviceCountWriteCommandService 设备计数写入命令服务
     * @param completionOrderReadService 完工单读取服务
     */
    @Autowired
    public IntegrationController(
            IntegrationService integrationService,
            DeviceCountWriteCommandService deviceCountWriteCommandService,
            CompletionOrderReadService completionOrderReadService,
            ObjectProvider<EquipmentBindingService> equipmentBindingServiceProvider,
            ObjectProvider<MaterialStockSyncService> materialStockSyncServiceProvider) {
        this.integrationService = integrationService;
        this.deviceCountWriteCommandService = deviceCountWriteCommandService;
        this.completionOrderReadService = completionOrderReadService;
        this.equipmentBindingService = equipmentBindingServiceProvider.getIfAvailable();
        this.materialStockSyncService = materialStockSyncServiceProvider.getIfAvailable();
    }

    /** 兼容既有 Controller 聚焦测试。 */
    public IntegrationController(
            IntegrationService integrationService,
            DeviceCountWriteCommandService deviceCountWriteCommandService,
            CompletionOrderReadService completionOrderReadService) {
        this(integrationService, deviceCountWriteCommandService,
                completionOrderReadService, emptyProvider(), emptyProvider());
    }

    private static <T> ObjectProvider<T> emptyProvider() {
        return new ObjectProvider<>() {
            @Override
            public T getObject(Object... args) {
                return null;
            }

            @Override
            public T getIfAvailable() {
                return null;
            }

            @Override
            public T getObject() {
                return null;
            }
        };
    }

    /**
     * 新增或更新计量单位。
     *
     * @param reqVO 单位写入请求
     * @return 写入结果
     */
    @PostMapping("/units")
    public CommonResult<IntegrationWriteResultRespVO> writeUnit(
            @Valid @RequestBody UnitWriteReqVO reqVO) {
        return CommonResult.success(integrationService.writeUnit(reqVO));
    }

    /**
     * 幂等写入外部生产工单。
     *
     * @param reqVO 外部工单写入请求
     * @return 写入结果
     */
    @PostMapping("/work_orders")
    public CommonResult<IntegrationWriteResultRespVO> writeWorkOrder(
            @Valid @RequestBody ExternalWorkOrderWriteReqVO reqVO) {
        return CommonResult.success(integrationService.writeWorkOrder(reqVO));
    }

    /**
     * 幂等写入外部生产任务单（派工单）。
     *
     * @param reqVO 外部任务单写入请求
     * @return 写入结果
     */
    @PostMapping("/dispatch_orders")
    public CommonResult<IntegrationWriteResultRespVO> writeDispatchOrder(
            @Valid @RequestBody ExternalDispatchOrderWriteReqVO reqVO) {
        return CommonResult.success(integrationService.writeDispatchOrder(reqVO));
    }

    /**
     * 幂等写入设备累计计数，匹配失败时进入异常池。
     *
     * @param reqVO 设备计数写入请求
     * @return 写入或异常池处理结果
     */
    @PostMapping("/device_counts")
    public CommonResult<IntegrationWriteResultRespVO> writeDeviceCount(
            @Valid @RequestBody DeviceCountWriteReqVO reqVO) {
        return CommonResult.success(integrationService.writeDeviceCount(reqVO));
    }

    /**
     * 分页查询设备计数异常池。
     *
     * @param reqVO 分页筛选条件
     * @return 设备计数异常分页
     */
    @GetMapping("/device_counts/exceptions")
    public CommonResult<PageResult<DeviceCountExceptionRespVO>> getDeviceCountExceptionPage(
            @Valid @ModelAttribute DeviceCountExceptionPageReqVO reqVO) {
        return CommonResult.success(deviceCountWriteCommandService.getExceptionPage(reqVO));
    }

    /** 保存设备报工绑定配置。 */
    @PostMapping("/equipment_bindings")
    public CommonResult<Long> saveEquipmentBinding(
            @Valid @RequestBody EquipmentBindingSaveReqVO reqVO) {
        return CommonResult.success(equipmentBindingService.saveBinding(reqVO));
    }

    /** 批量同步 WMS/ERP 库存与在途快照。 */
    @PostMapping("/material_stocks/batch")
    public CommonResult<Integer> syncMaterialStocks(
            @Valid @RequestBody MaterialStockBatchReqVO reqVO) {
        return CommonResult.success(materialStockSyncService.sync(reqVO));
    }

    /** 忽略一条待处理设备计数异常。 */
    @PutMapping("/device_counts/exceptions/{id}/ignore")
    public CommonResult<Boolean> ignoreDeviceCountException(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) DeviceExceptionActionReqVO reqVO) {
        deviceCountWriteCommandService.ignoreException(
                id, reqVO == null ? null : reqVO.getRemark());
        return CommonResult.success(true);
    }

    /** 使用修正后的请求重新处理设备计数异常。 */
    @PutMapping("/device_counts/exceptions/{id}/retry")
    public CommonResult<IntegrationWriteResultRespVO> retryDeviceCountException(
            @PathVariable Long id,
            @Valid @RequestBody DeviceCountWriteReqVO reqVO) {
        return CommonResult.success(
                deviceCountWriteCommandService.retryException(id, reqVO));
    }

    /**
     * 分页读取已审核生产完工单，并逐条记录读取日志。
     *
     * @param reqVO 分页筛选条件
     * @return 已审核完工单分页
     */
    @GetMapping("/completion_orders")
    public CommonResult<PageResult<CompletionOrderRespVO>> getCompletionOrderPage(
            @Valid @ModelAttribute CompletionOrderPageReqVO reqVO) {
        return CommonResult.success(completionOrderReadService.getCompletionOrderPage(reqVO));
    }

    /**
     * 分页查询生产完工单读取日志。
     *
     * @param reqVO 分页筛选条件
     * @return 读取日志分页
     */
    @GetMapping("/completion_orders/read_logs")
    public CommonResult<PageResult<CompletionReadLogRespVO>> getCompletionReadLogPage(
            @Valid @ModelAttribute CompletionReadLogPageReqVO reqVO) {
        return CommonResult.success(completionOrderReadService.getReadLogPage(reqVO));
    }

    /**
     * 分页查询外部接口写入结果。
     *
     * @param reqVO 分页筛选条件
     * @return 写入日志分页
     */
    @GetMapping("/write_logs")
    public CommonResult<PageResult<IntegrationWriteLogRespVO>> getWriteLogPage(
            @Valid @ModelAttribute IntegrationWriteLogPageReqVO reqVO) {
        return CommonResult.success(integrationService.getWriteLogPage(reqVO));
    }
}
