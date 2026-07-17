package com.badminton.mes.module.quality.service.impl;

import java.util.List;
import java.util.Optional;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.production.service.WorkOrderService;
import com.badminton.mes.module.quality.constants.QualityErrorCodeConstants;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionRecordSubmitReqVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionResultSaveReqVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionResultsSaveReqVO;
import com.badminton.mes.module.quality.dal.entity.QualityInspectionRecordEntity;
import com.badminton.mes.module.quality.dal.entity.QualityInspectionResultEntity;
import com.badminton.mes.module.quality.dal.redis.QualityCache;
import com.badminton.mes.module.quality.dal.redis.QualityRedisKeyConstants;
import com.badminton.mes.module.quality.dal.repository.QualityInspectionItemRepository;
import com.badminton.mes.module.quality.dal.repository.QualityInspectionPlanItemRepository;
import com.badminton.mes.module.quality.dal.repository.QualityInspectionPlanRepository;
import com.badminton.mes.module.quality.dal.repository.QualityInspectionRecordRepository;
import com.badminton.mes.module.quality.dal.repository.QualityInspectionResultRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link QualityInspectionRecordServiceImpl} 的单元测试。
 *
 * <p>通过 Mock 仓储、工单服务与质量缓存隔离数据库、跨模块调用及 Redis，重点覆盖草稿结果保存、
 * 结果完整性校验、检验单提交状态迁移，以及持久化成功后请求缓存组件安排详情失效的副作用。</p>
 */
@ExtendWith(MockitoExtension.class)
class QualityInspectionRecordServiceImplTest {

    /** 测试中固定使用的检验单主键，统一关联锁定查询与缓存键断言。 */
    private static final Long RECORD_ID = 100L;

    /** 测试中固定使用的检验结果主键，用于请求项与已存结果之间的匹配。 */
    private static final Long RESULT_ID = 200L;

    /** 隔离检验单的悲观锁查询与状态持久化。 */
    @Mock
    private QualityInspectionRecordRepository recordRepository;

    /** 隔离检验结果集合查询和批量保存。 */
    @Mock
    private QualityInspectionResultRepository resultRepository;

    /** 隔离提交流程可能访问的检验方案数据。 */
    @Mock
    private QualityInspectionPlanRepository planRepository;

    /** 隔离检验方案项目关系数据。 */
    @Mock
    private QualityInspectionPlanItemRepository planItemRepository;

    /** 隔离检验项目基础数据查询。 */
    @Mock
    private QualityInspectionItemRepository inspectionItemRepository;

    /** 隔离提交检验单时对生产工单模块的协作。 */
    @Mock
    private WorkOrderService workOrderService;

    /** 隔离 Redis，并用于验证事务提交后详情缓存失效请求。 */
    @Mock
    private QualityCache qualityCache;

    /** 使用上述 Mock 依赖组装的被测服务实例。 */
    private QualityInspectionRecordServiceImpl recordService;

    @BeforeEach
    void setUp() {
        recordService = new QualityInspectionRecordServiceImpl(
                recordRepository,
                resultRepository,
                planRepository,
                planItemRepository,
                inspectionItemRepository,
                workOrderService,
                qualityCache);
    }

    @Test
    @DisplayName("保存检验结果：更新草稿结果并失效检验单详情缓存")
    void saveResultsUpdatesStoredResultAndEvictsCache() {
        QualityInspectionRecordEntity record = buildRecord("DRAFT");
        QualityInspectionResultEntity storedResult = buildResult(true, null);
        QualityInspectionResultSaveReqVO resultRequest = new QualityInspectionResultSaveReqVO();
        resultRequest.setResultId(RESULT_ID);
        resultRequest.setMeasuredValue("合格");
        resultRequest.setJudgmentResult("PASS");
        QualityInspectionResultsSaveReqVO request = new QualityInspectionResultsSaveReqVO();
        request.setResults(List.of(resultRequest));
        when(recordRepository.findByIdAndDeletedFalseForUpdate(RECORD_ID))
                .thenReturn(Optional.of(record));
        // 返回与请求主键对应的持久化对象，验证服务是在原聚合上合并可变结果字段。
        when(resultRepository.findByInspectionRecordIdOrderBySortOrderAscIdAsc(RECORD_ID))
                .thenReturn(List.of(storedResult));

        recordService.saveResults(RECORD_ID, request);

        // 同时检查实体快照和仓储调用，防止仅触发保存却未把请求值写回实体。
        assertThat(storedResult.getMeasuredValue()).isEqualTo("合格");
        assertThat(storedResult.getJudgmentResult()).isEqualTo("PASS");
        verify(resultRepository).saveAll(any());
        // 缓存失效必须指向当前检验单，而不是误删其他质量资源的详情。
        verify(qualityCache).evictDetailAfterCommit(
                QualityRedisKeyConstants.INSPECTION_RECORD_RESOURCE,
                RECORD_ID);
    }

    @Test
    @DisplayName("保存检验结果：重复结果主键时拒绝保存")
    void saveResultsRejectsDuplicateResultIds() {
        QualityInspectionResultSaveReqVO firstResult = buildResultRequest(RESULT_ID, "PASS");
        QualityInspectionResultSaveReqVO duplicateResult = buildResultRequest(RESULT_ID, "PASS");
        QualityInspectionResultsSaveReqVO request = new QualityInspectionResultsSaveReqVO();
        request.setResults(List.of(firstResult, duplicateResult));
        when(recordRepository.findByIdAndDeletedFalseForUpdate(RECORD_ID))
                .thenReturn(Optional.of(buildRecord("DRAFT")));

        // 重复主键应在读取已存结果前被请求完整性校验拦截，避免产生部分更新。
        assertThatThrownBy(() -> recordService.saveResults(RECORD_ID, request))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isSameAs(
                                QualityErrorCodeConstants.RECORD_RESULTS_INCOMPLETE));
        verify(resultRepository, never())
                .findByInspectionRecordIdOrderBySortOrderAscIdAsc(RECORD_ID);
        verify(resultRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("提交检验单：全部合格时发布并失效详情缓存")
    void submitRecordReleasesPassingRecordAndEvictsCache() {
        QualityInspectionRecordEntity record = buildRecord("DRAFT");
        QualityInspectionResultEntity result = buildResult(true, "PASS");
        result.setMeasuredValue("合格");
        QualityInspectionRecordSubmitReqVO request = new QualityInspectionRecordSubmitReqVO();
        request.setConclusion("PASS");
        when(recordRepository.findByIdAndDeletedFalseForUpdate(RECORD_ID))
                .thenReturn(Optional.of(record));
        when(resultRepository.findByInspectionRecordIdOrderBySortOrderAscIdAsc(RECORD_ID))
                .thenReturn(List.of(result));

        recordService.submitRecord(RECORD_ID, request);

        // 验证提交状态机的完整快照：提交、结论、放行、检验人和检验时间必须同步落位。
        assertThat(record.getRecordStatus()).isEqualTo("SUBMITTED");
        assertThat(record.getConclusion()).isEqualTo("PASS");
        assertThat(record.getReleaseStatus()).isEqualTo("RELEASED");
        assertThat(record.getInspectorId()).isEqualTo(1L);
        assertThat(record.getInspectedAt()).isNotNull();
        verify(recordRepository).saveAndFlush(record);
        // 状态落库后应登记提交后失效动作，阻止详情缓存继续暴露草稿状态。
        verify(qualityCache).evictDetailAfterCommit(
                QualityRedisKeyConstants.INSPECTION_RECORD_RESOURCE,
                RECORD_ID);
    }

    @Test
    @DisplayName("提交检验单：存在失败项目时不允许使用合格结论")
    void submitRecordRejectsPassConclusionForFailedResult() {
        QualityInspectionRecordEntity record = buildRecord("DRAFT");
        QualityInspectionResultEntity failedResult = buildResult(true, "FAIL");
        failedResult.setMeasuredValue("裂纹");
        failedResult.setDefectDescription("表面存在明显裂纹");
        QualityInspectionRecordSubmitReqVO request = new QualityInspectionRecordSubmitReqVO();
        request.setConclusion("PASS");
        when(recordRepository.findByIdAndDeletedFalseForUpdate(RECORD_ID))
                .thenReturn(Optional.of(record));
        when(resultRepository.findByInspectionRecordIdOrderBySortOrderAscIdAsc(RECORD_ID))
                .thenReturn(List.of(failedResult));

        // 失败明细与合格结论冲突时，状态机必须保持在原状态且不得登记缓存副作用。
        assertThatThrownBy(() -> recordService.submitRecord(RECORD_ID, request))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isSameAs(
                                QualityErrorCodeConstants.RECORD_CONCLUSION_INVALID));
        verify(recordRepository, never()).saveAndFlush(any());
        verify(qualityCache, never()).evictDetailAfterCommit(any(), any());
    }

    @Test
    @DisplayName("提交检验单：不合格结论设置为阻断状态")
    void submitRecordBlocksFailedRecord() {
        QualityInspectionRecordEntity record = buildRecord("DRAFT");
        QualityInspectionResultEntity failedResult = buildResult(true, "FAIL");
        failedResult.setMeasuredValue("裂纹");
        failedResult.setDefectDescription("表面存在明显裂纹");
        QualityInspectionRecordSubmitReqVO request = new QualityInspectionRecordSubmitReqVO();
        request.setConclusion("REWORK");
        request.setNonconformanceDescription("成品表面开裂");
        request.setDisposition("返工后重新检验");
        request.setDefectQuantity(1);
        when(recordRepository.findByIdAndDeletedFalseForUpdate(RECORD_ID))
                .thenReturn(Optional.of(record));
        when(resultRepository.findByInspectionRecordIdOrderBySortOrderAscIdAsc(RECORD_ID))
                .thenReturn(List.of(failedResult));

        recordService.submitRecord(RECORD_ID, request);

        // 不合格提交仍完成检验单提交，但放行状态必须转为阻断并保留处置意见。
        assertThat(record.getRecordStatus()).isEqualTo("SUBMITTED");
        assertThat(record.getReleaseStatus()).isEqualTo("BLOCKED");
        assertThat(record.getDisposition()).isEqualTo("返工后重新检验");
        verify(recordRepository).saveAndFlush(record);
    }

    @Test
    @DisplayName("提交检验单：失败结果固化不良数量并规范化归并号")
    void submitRecordPersistsDefectQuantityAndNormalizedGroupNumber() {
        QualityInspectionRecordEntity record = buildRecord("DRAFT");
        QualityInspectionResultEntity failedResult = buildResult(true, "FAIL");
        failedResult.setMeasuredValue("NG");
        failedResult.setDefectDescription("外观破损");
        QualityInspectionRecordSubmitReqVO request = buildFailedSubmitRequest(3);
        when(recordRepository.findByIdAndDeletedFalseForUpdate(RECORD_ID))
                .thenReturn(Optional.of(record));
        when(resultRepository.findByInspectionRecordIdOrderBySortOrderAscIdAsc(RECORD_ID))
                .thenReturn(List.of(failedResult));

        recordService.submitRecord(RECORD_ID, request);

        assertThat(record.getRecordStatus()).isEqualTo("SUBMITTED");
        assertThat(record.getDefectQuantity()).isEqualTo(3);
        assertThat(record.getDefectGroupNo()).isEqualTo("DEFECT-001");
        verify(recordRepository).saveAndFlush(record);
        verify(qualityCache).evictDetailAfterCommit(
                QualityRedisKeyConstants.INSPECTION_RECORD_RESOURCE,
                RECORD_ID);
    }

    @Test
    @DisplayName("提交检验单：不良数量超过抽样数量时拒绝提交")
    void submitRecordRejectsDefectQuantityAboveSampleQuantity() {
        QualityInspectionRecordEntity record = buildRecord("DRAFT");
        QualityInspectionResultEntity failedResult = buildResult(true, "FAIL");
        failedResult.setMeasuredValue("NG");
        failedResult.setDefectDescription("外观破损");
        when(recordRepository.findByIdAndDeletedFalseForUpdate(RECORD_ID))
                .thenReturn(Optional.of(record));
        when(resultRepository.findByInspectionRecordIdOrderBySortOrderAscIdAsc(RECORD_ID))
                .thenReturn(List.of(failedResult));

        assertThatThrownBy(() -> recordService.submitRecord(
                RECORD_ID,
                buildFailedSubmitRequest(11)))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isSameAs(
                                QualityErrorCodeConstants.RECORD_RESULTS_INCOMPLETE));
        verify(recordRepository, never()).saveAndFlush(any());
        verify(qualityCache, never()).evictDetailAfterCommit(any(), any());
    }

    @Test
    @DisplayName("保存检验结果：已提交检验单不允许修改")
    void saveResultsRejectsSubmittedRecord() {
        QualityInspectionResultsSaveReqVO request = new QualityInspectionResultsSaveReqVO();
        request.setResults(List.of(buildResultRequest(RESULT_ID, "PASS")));
        when(recordRepository.findByIdAndDeletedFalseForUpdate(RECORD_ID))
                .thenReturn(Optional.of(buildRecord("SUBMITTED")));

        // 已离开草稿态后应由状态门禁立即拒绝，结果仓储不能收到任何写操作。
        assertThatThrownBy(() -> recordService.saveResults(RECORD_ID, request))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isSameAs(
                                QualityErrorCodeConstants.RECORD_EDIT_NOT_ALLOWED));
        verify(resultRepository, never()).saveAll(any());
    }

    /**
     * 构造最小可用检验单夹具，仅开放状态参数以复用草稿与已提交场景。
     */
    private QualityInspectionRecordEntity buildRecord(String recordStatus) {
        QualityInspectionRecordEntity record = new QualityInspectionRecordEntity();
        record.setId(RECORD_ID);
        record.setInspectionNo("QI-001");
        record.setRecordStatus(recordStatus);
        record.setSampleQuantity(10);
        record.setDeleted(false);
        return record;
    }

    /**
     * 构造归属于固定检验单的已存结果，用于驱动必填校验和结论汇总分支。
     */
    private QualityInspectionResultEntity buildResult(Boolean requiredFlag, String judgmentResult) {
        QualityInspectionResultEntity result = new QualityInspectionResultEntity();
        result.setId(RESULT_ID);
        result.setInspectionRecordId(RECORD_ID);
        result.setRequiredFlag(requiredFlag);
        result.setJudgmentResult(judgmentResult);
        return result;
    }

    /**
     * 构造结果保存请求项，集中提供有效测量值并按场景切换主键和判定结果。
     */
    private QualityInspectionResultSaveReqVO buildResultRequest(Long resultId, String judgmentResult) {
        QualityInspectionResultSaveReqVO request = new QualityInspectionResultSaveReqVO();
        request.setResultId(resultId);
        request.setMeasuredValue("合格");
        request.setJudgmentResult(judgmentResult);
        return request;
    }

    /** 构造具备完整不合格处置信息的提交请求，并保留归并号规范化的测试输入。 */
    private QualityInspectionRecordSubmitReqVO buildFailedSubmitRequest(int defectQuantity) {
        QualityInspectionRecordSubmitReqVO request = new QualityInspectionRecordSubmitReqVO();
        request.setConclusion("REWORK");
        request.setNonconformanceDescription("外观破损");
        request.setDisposition("返修");
        request.setDefectQuantity(defectQuantity);
        request.setDefectGroupNo(" DEFECT-001 ");
        return request;
    }
}
