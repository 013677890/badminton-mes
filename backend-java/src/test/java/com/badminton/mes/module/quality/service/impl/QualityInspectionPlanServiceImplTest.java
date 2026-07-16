package com.badminton.mes.module.quality.service.impl;

import java.util.List;
import java.util.Optional;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.quality.constants.QualityErrorCodeConstants;
import com.badminton.mes.module.quality.dal.entity.QualityInspectionPlanEntity;
import com.badminton.mes.module.quality.dal.entity.QualityInspectionPlanItemEntity;
import com.badminton.mes.module.quality.dal.redis.QualityCache;
import com.badminton.mes.module.quality.dal.redis.QualityRedisKeyConstants;
import com.badminton.mes.module.quality.dal.repository.QualityInspectionItemRepository;
import com.badminton.mes.module.quality.dal.repository.QualityInspectionPlanItemRepository;
import com.badminton.mes.module.quality.dal.repository.QualityInspectionPlanRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link QualityInspectionPlanServiceImpl} 的单元测试。
 *
 * <p>使用 Mock 仓储和质量缓存隔离数据库锁、批量持久化与 Redis，重点验证方案审核、停用和版本派生
 * 的状态约束、默认方案冲突保护、项目复制快照及调用缓存组件安排事务后失效的副作用。</p>
 */
@ExtendWith(MockitoExtension.class)
class QualityInspectionPlanServiceImplTest {

    /** 既有方案主键，贯穿方案查询、项目关联和缓存失效验证。 */
    private static final Long PLAN_ID = 100L;

    /** 模拟持久化层为派生版本分配的新方案主键。 */
    private static final Long NEW_PLAN_ID = 101L;

    /** 源方案项目引用的检验项目主键，用于核对复制关系未丢失。 */
    private static final Long INSPECTION_ITEM_ID = 200L;

    /** 隔离方案锁定查询、冲突检查及状态持久化。 */
    @Mock
    private QualityInspectionPlanRepository planRepository;

    /** 隔离方案项目计数、源项目读取和复制项目批量保存。 */
    @Mock
    private QualityInspectionPlanItemRepository planItemRepository;

    /** 隔离方案业务校验依赖的检验项目数据。 */
    @Mock
    private QualityInspectionItemRepository inspectionItemRepository;

    /** 隔离 Redis，并记录方案详情的提交后失效请求。 */
    @Mock
    private QualityCache qualityCache;

    /** 注入全部 Mock 协作者后的被测方案服务。 */
    private QualityInspectionPlanServiceImpl planService;

    @BeforeEach
    void setUp() {
        planService = new QualityInspectionPlanServiceImpl(
                planRepository,
                planItemRepository,
                inspectionItemRepository,
                qualityCache);
    }

    @Test
    @DisplayName("审核检验方案：草稿包含项目时生效并失效详情缓存")
    void auditPlanMakesDraftEffectiveAndEvictsCache() {
        QualityInspectionPlanEntity plan = buildPlan("DRAFT", 1);
        when(planRepository.findByIdAndDeletedFalseForUpdate(PLAN_ID))
                .thenReturn(Optional.of(plan));
        when(planItemRepository.countByPlanId(PLAN_ID)).thenReturn(1L);

        planService.auditPlan(PLAN_ID);

        // 审核状态机除切换主状态外，还必须一次性补齐生效与审计字段。
        assertThat(plan.getPlanStatus()).isEqualTo("EFFECTIVE");
        assertThat(plan.getEffectiveDate()).isNotNull();
        assertThat(plan.getAuditBy()).isEqualTo(1L);
        assertThat(plan.getAuditTime()).isNotNull();
        verify(planRepository).lockPlansByInspectionType("PATROL");
        verify(planRepository).saveAndFlush(plan);
        // 缓存副作用应绑定被审核方案，确保旧草稿快照不会在提交后继续被读取。
        verify(qualityCache).evictDetailAfterCommit(
                QualityRedisKeyConstants.INSPECTION_PLAN_RESOURCE,
                PLAN_ID);
    }

    @Test
    @DisplayName("审核默认方案：同适用范围已有生效默认方案时拒绝审核")
    void auditPlanRejectsEffectiveDefaultConflict() {
        QualityInspectionPlanEntity plan = buildPlan("DRAFT", 1);
        plan.setDefaultFlag(true);
        plan.setProductId(10L);
        when(planRepository.findByIdAndDeletedFalseForUpdate(PLAN_ID))
                .thenReturn(Optional.of(plan));
        when(planItemRepository.countByPlanId(PLAN_ID)).thenReturn(1L);
        when(planRepository.existsEffectiveDefaultForScope(10L, null, "PATROL", PLAN_ID))
                .thenReturn(true);

        // 在同范围默认方案冲突时终止状态迁移，持久化和缓存失效均不应发生。
        assertThatThrownBy(() -> planService.auditPlan(PLAN_ID))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isSameAs(
                                QualityErrorCodeConstants.PLAN_DEFAULT_CONFLICT));
        verify(planRepository, never()).saveAndFlush(any());
        verify(qualityCache, never()).evictDetailAfterCommit(any(), any());
    }

    @Test
    @DisplayName("停用检验方案：生效方案转为停用并取消默认标记")
    void disablePlanChangesStatusAndClearsDefaultFlag() {
        QualityInspectionPlanEntity plan = buildPlan("EFFECTIVE", 1);
        plan.setDefaultFlag(true);
        when(planRepository.findByIdAndDeletedFalseForUpdate(PLAN_ID))
                .thenReturn(Optional.of(plan));

        planService.disablePlan(PLAN_ID);

        // 停用状态与默认标记构成同一业务快照，避免不可用方案继续参与默认匹配。
        assertThat(plan.getPlanStatus()).isEqualTo("DISABLED");
        assertThat(plan.getDefaultFlag()).isFalse();
        verify(planRepository).saveAndFlush(plan);
        verify(qualityCache).evictDetailAfterCommit(
                QualityRedisKeyConstants.INSPECTION_PLAN_RESOURCE,
                PLAN_ID);
    }

    @Test
    @DisplayName("创建新版本：锁定同编码版本并复制方案项目为新草稿")
    void createNewVersionCopiesPlanAndItemsAsDraft() {
        QualityInspectionPlanEntity sourcePlan = buildPlan("EFFECTIVE", 1);
        QualityInspectionPlanEntity secondVersion = buildPlan("DISABLED", 2);
        secondVersion.setId(99L);
        QualityInspectionPlanItemEntity sourceItem = buildPlanItem();
        when(planRepository.findByIdAndDeletedFalse(PLAN_ID))
                .thenReturn(Optional.of(sourcePlan));
        when(planRepository.lockPlansByPlanCode("PLAN-001"))
                .thenReturn(List.of(sourcePlan, secondVersion));
        when(planItemRepository.findByPlanIdOrderBySortOrderAscIdAsc(PLAN_ID))
                .thenReturn(List.of(sourceItem));
        // 模拟数据库生成主键，使后续项目复制能够引用真实的新版本标识。
        when(planRepository.saveAndFlush(any(QualityInspectionPlanEntity.class)))
                .thenAnswer(invocation -> {
                    QualityInspectionPlanEntity savedPlan = invocation.getArgument(0);
                    if (savedPlan.getId() == null) {
                        savedPlan.setId(NEW_PLAN_ID);
                    }
                    return savedPlan;
                });

        Long newVersionId = planService.createNewVersion(PLAN_ID);

        assertThat(newVersionId).isEqualTo(NEW_PLAN_ID);
        // 捕获实际写入的新方案快照，避免仅根据返回主键判断版本复制是否正确。
        ArgumentCaptor<QualityInspectionPlanEntity> planCaptor =
                ArgumentCaptor.forClass(QualityInspectionPlanEntity.class);
        verify(planRepository).saveAndFlush(planCaptor.capture());
        QualityInspectionPlanEntity newVersion = planCaptor.getValue();
        assertThat(newVersion.getVersionNo()).isEqualTo(3);
        assertThat(newVersion.getPlanStatus()).isEqualTo("DRAFT");
        assertThat(newVersion.getDefaultFlag()).isFalse();
        assertThat(newVersion.getEffectiveDate()).isNull();

        // 泛型在运行期被擦除，Captor 用于核对批量写入的复制项目及其新父级关联。
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<QualityInspectionPlanItemEntity>> itemCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(planItemRepository).saveAllAndFlush(itemCaptor.capture());
        assertThat(itemCaptor.getValue()).singleElement().satisfies(copiedItem -> {
            assertThat(copiedItem.getPlanId()).isEqualTo(NEW_PLAN_ID);
            assertThat(copiedItem.getInspectionItemId()).isEqualTo(INSPECTION_ITEM_ID);
            assertThat(copiedItem.getSampleQuantity()).isEqualTo(5);
        });
    }

    @Test
    @DisplayName("创建新版本：草稿方案不允许再次派生版本")
    void createNewVersionRejectsDraftSource() {
        QualityInspectionPlanEntity sourcePlan = buildPlan("DRAFT", 1);
        when(planRepository.findByIdAndDeletedFalse(PLAN_ID))
                .thenReturn(Optional.of(sourcePlan));
        when(planRepository.lockPlansByPlanCode("PLAN-001"))
                .thenReturn(List.of(sourcePlan));

        // 草稿源不满足派生前置状态，必须在创建新方案及复制项目之前失败。
        assertThatThrownBy(() -> planService.createNewVersion(PLAN_ID))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isSameAs(
                                QualityErrorCodeConstants.PLAN_VERSION_NOT_ALLOWED));
        verify(planRepository, never()).saveAndFlush(any());
        verify(planItemRepository, never()).saveAllAndFlush(any());
    }

    /**
     * 构造具有固定业务编码的方案夹具，通过状态和版本参数驱动生命周期分支。
     */
    private QualityInspectionPlanEntity buildPlan(String planStatus, Integer versionNo) {
        QualityInspectionPlanEntity plan = new QualityInspectionPlanEntity();
        plan.setId(PLAN_ID);
        plan.setPlanCode("PLAN-001");
        plan.setPlanName("巡检方案");
        plan.setInspectionType("PATROL");
        plan.setVersionNo(versionNo);
        plan.setPlanStatus(planStatus);
        plan.setDefaultFlag(false);
        plan.setDeleted(false);
        return plan;
    }

    /**
     * 构造源方案中的完整项目快照，用于验证派生版本保留排序、抽样和判定配置。
     */
    private QualityInspectionPlanItemEntity buildPlanItem() {
        QualityInspectionPlanItemEntity planItem = new QualityInspectionPlanItemEntity();
        planItem.setId(300L);
        planItem.setPlanId(PLAN_ID);
        planItem.setInspectionItemId(INSPECTION_ITEM_ID);
        planItem.setSortOrder(1);
        planItem.setSampleQuantity(5);
        planItem.setRequiredFlag(true);
        planItem.setJudgmentMethod("MANUAL");
        return planItem;
    }
}
