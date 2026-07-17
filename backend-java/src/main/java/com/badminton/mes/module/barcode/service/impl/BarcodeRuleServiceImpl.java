package com.badminton.mes.module.barcode.service.impl;

import java.time.LocalDate;
import java.util.List;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.enums.CommonStatusEnum;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.barcode.constants.BarcodeErrorCodeConstants;
import com.badminton.mes.module.barcode.controller.vo.BarcodeRulePageReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeRulePreviewReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeRulePreviewRespVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeRuleRespVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeRuleSaveReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeRuleValidateReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeRuleValidateRespVO;
import com.badminton.mes.module.barcode.convert.BarcodeRuleConvert;
import com.badminton.mes.module.barcode.dal.entity.BarcodeRuleEntity;
import com.badminton.mes.module.barcode.dal.entity.BarcodeTypeEntity;
import com.badminton.mes.module.barcode.dal.repository.BarcodeApplyRuleRepository;
import com.badminton.mes.module.barcode.dal.repository.BarcodeRuleItemRepository;
import com.badminton.mes.module.barcode.dal.repository.BarcodeRuleRepository;
import com.badminton.mes.module.barcode.dal.repository.BarcodeRuleSpecifications;
import com.badminton.mes.module.barcode.dal.repository.BarcodeSerialRepository;
import com.badminton.mes.module.barcode.dal.repository.BarcodeTypeRepository;
import com.badminton.mes.module.barcode.service.BarcodeRuleService;
import com.badminton.mes.module.barcode.service.impl.BarcodeValueComposer.ComposeContext;
import com.badminton.mes.module.barcode.service.impl.BarcodeValueComposer.ComposedSegment;
import com.badminton.mes.module.barcode.service.impl.BarcodeValueComposer.RuleSegment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 条码规则 Service 实现。
 *
 * <p>规则与组成明细在同一事务内保存；修改整体重写明细，只影响新生成条码。
 * 预览与校验复用 {@link BarcodeValueComposer}，保证"预览即所得"。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Service
public class BarcodeRuleServiceImpl implements BarcodeRuleService {

    private static final Logger logger = LoggerFactory.getLogger(BarcodeRuleServiceImpl.class);

    /** 预览使用的样例流水号，不消耗真实流水 */
    private static final long PREVIEW_SAMPLE_SERIAL = 1L;

    private final BarcodeRuleRepository barcodeRuleRepository;

    private final BarcodeRuleItemRepository barcodeRuleItemRepository;

    private final BarcodeTypeRepository barcodeTypeRepository;

    private final BarcodeApplyRuleRepository barcodeApplyRuleRepository;

    private final BarcodeSerialRepository barcodeSerialRepository;

    /**
     * 构造器注入：依赖不可变、便于单测中直接 new 出被测对象。
     *
     * @param barcodeRuleRepository      条码规则 Repository
     * @param barcodeRuleItemRepository  规则组成明细 Repository
     * @param barcodeTypeRepository      条码类型 Repository，引用可用性校验
     * @param barcodeApplyRuleRepository 应用规则 Repository，删除前引用校验
     * @param barcodeSerialRepository    流水记录 Repository，删除前使用痕迹校验
     */
    public BarcodeRuleServiceImpl(BarcodeRuleRepository barcodeRuleRepository,
                                  BarcodeRuleItemRepository barcodeRuleItemRepository,
                                  BarcodeTypeRepository barcodeTypeRepository,
                                  BarcodeApplyRuleRepository barcodeApplyRuleRepository,
                                  BarcodeSerialRepository barcodeSerialRepository) {
        this.barcodeRuleRepository = barcodeRuleRepository;
        this.barcodeRuleItemRepository = barcodeRuleItemRepository;
        this.barcodeTypeRepository = barcodeTypeRepository;
        this.barcodeApplyRuleRepository = barcodeApplyRuleRepository;
        this.barcodeSerialRepository = barcodeSerialRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createBarcodeRule(BarcodeRuleSaveReqVO reqVO) {
        // 保存前先验证所属类型和全部组成段，确保主表不会先落下一条无法生成条码的半成品规则。
        validateBarcodeTypeAvailable(reqVO.getBarcodeTypeId());
        validateRuleConfig(reqVO);
        if (barcodeRuleRepository.existsByRuleCodeAndDeletedFalse(reqVO.getRuleCode())) {
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_RULE_CODE_DUPLICATE);
        }

        // 主表先 flush 取得稳定主键并触发编码唯一约束，随后明细才能正确关联 rule_id。
        BarcodeRuleEntity rule = BarcodeRuleConvert.toEntity(reqVO);
        rule.setStatus(CommonStatusEnum.ENABLED.getStatus());
        try {
            barcodeRuleRepository.saveAndFlush(rule);
        } catch (DataIntegrityViolationException e) {
            // 并发创建穿透应用层查重时，由唯一索引 uk_rule_code 兜底转业务错误
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_RULE_CODE_DUPLICATE);
        }
        // 所有组成段与规则主表处于同一事务，任一明细写入失败都会回滚整条规则。
        barcodeRuleItemRepository.saveAll(BarcodeRuleConvert.toItemEntities(rule.getId(), reqVO.getItems()));

        logger.info("[创建条码规则] id: {}, ruleCode: {}", rule.getId(), rule.getRuleCode());
        return rule.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBarcodeRule(Long id, BarcodeRuleSaveReqVO reqVO) {
        // 存在性、类型可用性、段配置和编码唯一性全部通过后才执行数据库更新。
        validateBarcodeRuleExists(id);
        validateBarcodeTypeAvailable(reqVO.getBarcodeTypeId());
        validateRuleConfig(reqVO);
        if (barcodeRuleRepository.existsByRuleCodeAndIdNotAndDeletedFalse(reqVO.getRuleCode(), id)) {
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_RULE_CODE_DUPLICATE);
        }

        int rows;
        try {
            // 条件更新只修改未删除记录；并发删除发生后返回 0，防止把已删除规则重新写活。
            rows = barcodeRuleRepository.updateInfo(id, reqVO.getRuleCode(), reqVO.getRuleName(),
                    reqVO.getBarcodeTypeId(), reqVO.getSerialLength(), reqVO.getSerialResetCycle());
        } catch (DataIntegrityViolationException e) {
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_RULE_CODE_DUPLICATE);
        }
        // CAS 未命中：校验与更新的间隙内规则被并发删除
        if (rows == 0) {
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_RULE_NOT_EXISTS);
        }

        // 组成明细整体重写；历史条码保存最终值，不受明细重写影响(已冻结决策)
        // 先逻辑删除旧明细再写入新明细，事务提交后其他请求只能看到完整的新版本配置。
        barcodeRuleItemRepository.logicDeleteByRuleId(id);
        barcodeRuleItemRepository.saveAll(BarcodeRuleConvert.toItemEntities(id, reqVO.getItems()));
        logger.info("[修改条码规则] id: {}, ruleCode: {}", id, reqVO.getRuleCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableBarcodeRule(Long id) {
        // CAS 仅允许停用状态转为启用，重复启用不会覆盖并发维护结果。
        int rows = barcodeRuleRepository.updateStatus(id, CommonStatusEnum.DISABLED.getStatus(),
                CommonStatusEnum.ENABLED.getStatus());
        if (rows == 0) {
            validateBarcodeRuleExists(id);
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_RULE_ALREADY_ENABLED);
        }

        logger.info("[启用条码规则] id: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableBarcodeRule(Long id) {
        // CAS 仅允许启用状态转为停用，并在未命中时回查区分不存在与重复停用。
        int rows = barcodeRuleRepository.updateStatus(id, CommonStatusEnum.ENABLED.getStatus(),
                CommonStatusEnum.DISABLED.getStatus());
        if (rows == 0) {
            validateBarcodeRuleExists(id);
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_RULE_ALREADY_DISABLED);
        }

        logger.info("[停用条码规则] id: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBarcodeRule(Long id) {
        validateBarcodeRuleExists(id);
        // 接口契约"删除未使用规则"：被应用规则引用，或已产生流水(生成过条码)均视为已使用
        boolean inUse = barcodeApplyRuleRepository.existsByRuleIdAndDeletedFalse(id)
                || barcodeSerialRepository.existsByRuleIdAndDeletedFalse(id);
        // 应用规则引用或流水事实任一存在都说明规则已进入业务历史，不能再逻辑删除。
        if (inUse) {
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_RULE_IN_USE_NOT_DELETE);
        }

        int rows = barcodeRuleRepository.logicDeleteById(id);
        if (rows == 0) {
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_RULE_NOT_EXISTS);
        }

        // 组成明细随规则一起失效，避免残留半套配置
        barcodeRuleItemRepository.logicDeleteByRuleId(id);
        logger.info("[删除条码规则] id: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public BarcodeRuleRespVO getBarcodeRule(Long id) {
        BarcodeRuleEntity rule = validateBarcodeRuleExists(id);
        return BarcodeRuleConvert.toRespVO(rule,
                barcodeRuleItemRepository.findByRuleIdAndDeletedFalseOrderBySeqAsc(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<BarcodeRuleRespVO> getBarcodeRulePage(BarcodeRulePageReqVO reqVO) {
        Specification<BarcodeRuleEntity> specification = BarcodeRuleSpecifications.page(reqVO);
        // 无匹配记录时直接返回空分页，避免继续执行内容 SQL。
        long total = barcodeRuleRepository.count(specification);
        if (total == 0) {
            return PageResult.empty(reqVO.getPageNo(), reqVO.getPageSize());
        }

        int pageSize = reqVO.getPageSize();
        int totalPages = (int) ((total + pageSize - 1) / pageSize);
        int pageNo = Math.min(reqVO.getPageNo(), totalPages);
        PageRequest pageRequest = PageRequest.of(pageNo - 1, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        Page<BarcodeRuleEntity> page = barcodeRuleRepository.findAll(specification, pageRequest);
        return PageResult.of(BarcodeRuleConvert.toRespVOList(page.getContent()), total, pageNo, pageSize);
    }

    @Override
    public BarcodeRulePreviewRespVO previewBarcodeRule(BarcodeRulePreviewReqVO reqVO) {
        // 预览复用正式生成的校验器和组合器，但使用固定样例流水，不消耗 Redis/MySQL 真实序号。
        List<RuleSegment> segments = reqVO.getItems().stream().map(RuleSegment::from).toList();
        List<String> errors = BarcodeValueComposer.validate(reqVO.getSerialLength(), segments);
        if (!errors.isEmpty()) {
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_RULE_CONFIG_INVALID,
                    String.join("；", errors));
        }

        ComposeContext context = new ComposeContext(LocalDate.now(), reqVO.getSampleProductCode(),
                reqVO.getSampleLineCode(), PREVIEW_SAMPLE_SERIAL, reqVO.getSerialLength());
        List<ComposedSegment> composedSegments = BarcodeValueComposer.composeSegments(segments, context);

        BarcodeRulePreviewRespVO respVO = new BarcodeRulePreviewRespVO();
        StringBuilder barcodeValue = new StringBuilder();
        // 转换每个组成段的同时按顺序拼接最终条码，响应保留分段信息便于界面解释来源。
        respVO.setSegments(composedSegments.stream().map(segment -> {
            BarcodeRulePreviewRespVO.Segment segmentVO = new BarcodeRulePreviewRespVO.Segment();
            segmentVO.setSeq(segment.seq());
            segmentVO.setItemType(segment.itemType());
            segmentVO.setContent(segment.content());
            barcodeValue.append(segment.content());
            return segmentVO;
        }).toList());
        respVO.setBarcodeValue(barcodeValue.toString());
        respVO.setTotalLength(barcodeValue.length());
        respVO.setSerialCapacity(BarcodeValueComposer.serialCapacity(reqVO.getSerialLength()));
        return respVO;
    }

    @Override
    public BarcodeRuleValidateRespVO validateBarcodeRule(BarcodeRuleValidateReqVO reqVO) {
        // 校验接口只返回结构化错误列表，不访问数据库，也不产生任何流水或规则数据。
        List<RuleSegment> segments = reqVO.getItems().stream().map(RuleSegment::from).toList();
        List<String> errors = BarcodeValueComposer.validate(reqVO.getSerialLength(), segments);

        BarcodeRuleValidateRespVO respVO = new BarcodeRuleValidateRespVO();
        respVO.setValid(errors.isEmpty());
        respVO.setErrors(errors);
        return respVO;
    }

    /**
     * 校验规则组成配置合法性，不合法时抛业务异常并携带逐条错误。
     *
     * @param reqVO 保存请求
     */
    private void validateRuleConfig(BarcodeRuleSaveReqVO reqVO) {
        List<RuleSegment> segments = reqVO.getItems().stream().map(RuleSegment::from).toList();
        List<String> errors = BarcodeValueComposer.validate(reqVO.getSerialLength(), segments);
        if (!errors.isEmpty()) {
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_RULE_CONFIG_INVALID,
                    String.join("；", errors));
        }
    }

    /**
     * 校验条码类型存在且处于启用状态(停用类型不再出现在规则配置选项中)。
     *
     * @param barcodeTypeId 条码类型主键
     */
    private void validateBarcodeTypeAvailable(Long barcodeTypeId) {
        BarcodeTypeEntity barcodeType = barcodeTypeRepository.findByIdAndDeletedFalse(barcodeTypeId)
                .orElseThrow(() -> new ServiceException(BarcodeErrorCodeConstants.BARCODE_TYPE_NOT_AVAILABLE));
        if (!CommonStatusEnum.ENABLED.getStatus().equals(barcodeType.getStatus())) {
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_TYPE_NOT_AVAILABLE);
        }
    }

    /**
     * 校验条码规则存在且未删除。
     *
     * @param id 规则主键
     * @return 规则实体
     */
    private BarcodeRuleEntity validateBarcodeRuleExists(Long id) {
        return barcodeRuleRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException(BarcodeErrorCodeConstants.BARCODE_RULE_NOT_EXISTS));
    }
}
