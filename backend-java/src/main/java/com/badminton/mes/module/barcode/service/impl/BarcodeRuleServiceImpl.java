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
        validateBarcodeTypeAvailable(reqVO.getBarcodeTypeId());
        validateRuleConfig(reqVO);
        if (barcodeRuleRepository.existsByRuleCodeAndDeletedFalse(reqVO.getRuleCode())) {
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_RULE_CODE_DUPLICATE);
        }

        BarcodeRuleEntity rule = BarcodeRuleConvert.toEntity(reqVO);
        rule.setStatus(CommonStatusEnum.ENABLED.getStatus());
        try {
            barcodeRuleRepository.saveAndFlush(rule);
        } catch (DataIntegrityViolationException e) {
            // 并发创建穿透应用层查重时，由唯一索引 uk_rule_code 兜底转业务错误
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_RULE_CODE_DUPLICATE);
        }
        barcodeRuleItemRepository.saveAll(BarcodeRuleConvert.toItemEntities(rule.getId(), reqVO.getItems()));

        logger.info("[创建条码规则] id: {}, ruleCode: {}", rule.getId(), rule.getRuleCode());
        return rule.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBarcodeRule(Long id, BarcodeRuleSaveReqVO reqVO) {
        validateBarcodeRuleExists(id);
        validateBarcodeTypeAvailable(reqVO.getBarcodeTypeId());
        validateRuleConfig(reqVO);
        if (barcodeRuleRepository.existsByRuleCodeAndIdNotAndDeletedFalse(reqVO.getRuleCode(), id)) {
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_RULE_CODE_DUPLICATE);
        }

        int rows;
        try {
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
        barcodeRuleItemRepository.logicDeleteByRuleId(id);
        barcodeRuleItemRepository.saveAll(BarcodeRuleConvert.toItemEntities(id, reqVO.getItems()));
        logger.info("[修改条码规则] id: {}, ruleCode: {}", id, reqVO.getRuleCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableBarcodeRule(Long id) {
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
