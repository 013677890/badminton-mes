package com.badminton.mes.module.integration.service;

import java.time.LocalDateTime;
import java.util.List;

import com.badminton.mes.module.integration.service.dto.ErpCraftDTO;
import com.badminton.mes.module.integration.service.dto.ErpCraftStepDTO;
import com.badminton.mes.module.integration.service.dto.ErpTaskDTO;

import org.springframework.stereotype.Component;

/**
 * ERP Mock 数据源，模拟 ERP 系统返回的生产任务单和工艺路线数据。
 *
 * <p>硬编码示例数据，覆盖正常、异常（产品不存在/数量非法/工序顺序问题）和重复场景。
 * 后续可替换为真实 ERP 客户端实现。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Component
public class ErpMockDataSource {

    /** 默认来源系统标识 */
    public static final String DEFAULT_SOURCE_SYSTEM = "ERP";

    /**
     * 模拟从 ERP 拉取生产任务单列表。
     *
     * @return ERP 任务单列表
     */
    public List<ErpTaskDTO> fetchTasks() {
        LocalDateTime baseTime = LocalDateTime.of(2026, 7, 13, 8, 0, 0);
        return List.of(
                // 正常任务：产品 P001，车间 WS001
                new ErpTaskDTO("ERP-TASK-001", "P001", 1000,
                        baseTime, baseTime.plusDays(7), "WS001", "BATCH-001"),
                // 正常任务：产品 P002，车间 WS002
                new ErpTaskDTO("ERP-TASK-002", "P002", 500,
                        baseTime, baseTime.plusDays(5), "WS002", null),
                // 异常任务：产品不存在
                new ErpTaskDTO("ERP-TASK-003", "P999", 200,
                        baseTime, baseTime.plusDays(3), "WS001", null),
                // 异常任务：数量 ≤ 0
                new ErpTaskDTO("ERP-TASK-004", "P001", 0,
                        baseTime, baseTime.plusDays(3), "WS001", null),
                // 异常任务：计划完成时间早于开始时间
                new ErpTaskDTO("ERP-TASK-005", "P002", 300,
                        baseTime.plusDays(5), baseTime, "WS002", null)
        );
    }

    /**
     * 模拟从 ERP 拉取工艺路线列表。
     *
     * @return ERP 工艺路线列表
     */
    public List<ErpCraftDTO> fetchCrafts() {
        return List.of(
                // 正常工艺：产品 P001，工序顺序 1-5 完整
                new ErpCraftDTO("ERP-ROUTE-001", "比赛级羽毛球工艺路线", "V1.0", "P001",
                        List.of(
                                new ErpCraftStepDTO(1, "PR001", "羽毛分拣"),
                                new ErpCraftStepDTO(2, "PR002", "插毛成型"),
                                new ErpCraftStepDTO(3, "PR003", "注胶固定"),
                                new ErpCraftStepDTO(4, "PR004", "质量检验"),
                                new ErpCraftStepDTO(5, "PR005", "包装入库"))),
                // 正常工艺：产品 P002，工序顺序 1-3 完整
                new ErpCraftDTO("ERP-ROUTE-002", "训练级羽毛球工艺路线", "V1.0", "P002",
                        List.of(
                                new ErpCraftStepDTO(1, "PR001", "羽毛分拣"),
                                new ErpCraftStepDTO(2, "PR002", "插毛成型"),
                                new ErpCraftStepDTO(3, "PR005", "包装入库"))),
                // 异常工艺：产品不存在
                new ErpCraftDTO("ERP-ROUTE-003", "未知产品工艺路线", "V1.0", "P999",
                        List.of(
                                new ErpCraftStepDTO(1, "PR001", "羽毛分拣"),
                                new ErpCraftStepDTO(2, "PR002", "插毛成型"))),
                // 异常工艺：工序顺序不连续（缺少 2）
                new ErpCraftDTO("ERP-ROUTE-004", "顺序异常工艺路线", "V1.0", "P001",
                        List.of(
                                new ErpCraftStepDTO(1, "PR001", "羽毛分拣"),
                                new ErpCraftStepDTO(3, "PR003", "注胶固定"))),
                // 异常工艺：工序顺序重复
                new ErpCraftDTO("ERP-ROUTE-005", "重复顺序工艺路线", "V1.0", "P002",
                        List.of(
                                new ErpCraftStepDTO(1, "PR001", "羽毛分拣"),
                                new ErpCraftStepDTO(1, "PR002", "插毛成型")))
        );
    }
}
