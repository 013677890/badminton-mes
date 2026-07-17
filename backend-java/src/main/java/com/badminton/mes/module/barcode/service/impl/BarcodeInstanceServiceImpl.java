package com.badminton.mes.module.barcode.service.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.enums.CommonStatusEnum;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.LoginUser;
import com.badminton.mes.common.security.RoleCodeConstants;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.barcode.constants.BarcodeErrorCodeConstants;
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
import com.badminton.mes.module.barcode.controller.vo.BarcodeTemplatePreviewRespVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeUseRecordRespVO;
import com.badminton.mes.module.barcode.convert.BarcodeInstanceConvert;
import com.badminton.mes.module.barcode.convert.BarcodeTemplateConvert;
import com.badminton.mes.module.barcode.dal.entity.BarcodeApplyRuleEntity;
import com.badminton.mes.module.barcode.dal.entity.BarcodeEntity;
import com.badminton.mes.module.barcode.dal.entity.BarcodePrintRecordEntity;
import com.badminton.mes.module.barcode.dal.entity.BarcodeRuleEntity;
import com.badminton.mes.module.barcode.dal.entity.BarcodeSerialEntity;
import com.badminton.mes.module.barcode.dal.entity.BarcodeTemplateEntity;
import com.badminton.mes.module.barcode.dal.entity.BarcodeTypeEntity;
import com.badminton.mes.module.barcode.dal.entity.MaterialRefEntity;
import com.badminton.mes.module.barcode.dal.entity.ProductRefEntity;
import com.badminton.mes.module.barcode.dal.entity.WorkOrderRefEntity;
import com.badminton.mes.module.barcode.dal.redis.BarcodeSerialSequence;
import com.badminton.mes.module.barcode.dal.repository.BarcodeApplyRuleRepository;
import com.badminton.mes.module.barcode.dal.repository.BarcodePrintRecordRepository;
import com.badminton.mes.module.barcode.dal.repository.BarcodeRepository;
import com.badminton.mes.module.barcode.dal.repository.BarcodeRuleItemRepository;
import com.badminton.mes.module.barcode.dal.repository.BarcodeRuleRepository;
import com.badminton.mes.module.barcode.dal.repository.BarcodeSerialRepository;
import com.badminton.mes.module.barcode.dal.repository.BarcodeSpecifications;
import com.badminton.mes.module.barcode.dal.repository.BarcodeTemplateFieldRepository;
import com.badminton.mes.module.barcode.dal.repository.BarcodeTemplateRepository;
import com.badminton.mes.module.barcode.dal.repository.BarcodeTypeRepository;
import com.badminton.mes.module.barcode.dal.repository.BarcodeUseRecordRepository;
import com.badminton.mes.module.barcode.dal.repository.MaterialRefRepository;
import com.badminton.mes.module.barcode.dal.repository.ProductRefRepository;
import com.badminton.mes.module.barcode.dal.repository.WorkOrderRefRepository;
import com.badminton.mes.module.barcode.enums.BarcodeModeEnum;
import com.badminton.mes.module.barcode.enums.BarcodeSerialResetCycleEnum;
import com.badminton.mes.module.barcode.enums.BarcodeSourceTypeEnum;
import com.badminton.mes.module.barcode.enums.BarcodeStatusEnum;
import com.badminton.mes.module.barcode.service.BarcodeInstanceService;
import com.badminton.mes.module.barcode.service.impl.BarcodeValueComposer.ComposeContext;
import com.badminton.mes.module.barcode.service.impl.BarcodeValueComposer.RuleSegment;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 条码实例 Service 实现。
 *
 * <p>生成路径：校验应用规则及三档案启用 → 构造流水作用域 → Redis 取号 →
 * 组合条码值 → 预检查重后落库，barcode 唯一索引兜底并发窗口；落库成功后
 * 推进 MySQL 流水事实。MySQL 是事实数据源，Redis 只分配流水(已冻结决策)。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Service
public class BarcodeInstanceServiceImpl implements BarcodeInstanceService {

    private static final Logger logger = LoggerFactory.getLogger(BarcodeInstanceServiceImpl.class);

    /** 规则生成撞码时的取号重试上限(预检查重路径) */
    private static final int GENERATE_MAX_RETRY = 3;

    /** 流水作用域分隔符：周期日期段 + 对象编码 */
    private static final String SCOPE_SEPARATOR = ":";

    /** 流水作用域长度上限，与 barcode_serial.serial_scope varchar(64) 一致 */
    private static final int SERIAL_SCOPE_MAX_LENGTH = 64;

    private final BarcodeRepository barcodeRepository;

    private final BarcodeApplyRuleRepository barcodeApplyRuleRepository;

    private final BarcodeTypeRepository barcodeTypeRepository;

    private final BarcodeRuleRepository barcodeRuleRepository;

    private final BarcodeRuleItemRepository barcodeRuleItemRepository;

    private final BarcodeTemplateRepository barcodeTemplateRepository;

    private final BarcodeSerialRepository barcodeSerialRepository;

    private final BarcodePrintRecordRepository barcodePrintRecordRepository;

    private final BarcodeTemplateFieldRepository barcodeTemplateFieldRepository;

    private final BarcodeUseRecordRepository barcodeUseRecordRepository;

    private final ProductRefRepository productRefRepository;

    private final MaterialRefRepository materialRefRepository;

    private final WorkOrderRefRepository workOrderRefRepository;

    private final BarcodeSerialSequence barcodeSerialSequence;

    private final ObjectMapper objectMapper;

    /**
     * 构造器注入：依赖不可变、便于单测中直接 new 出被测对象。
     *
     * @param barcodeRepository              条码主表 Repository
     * @param barcodeApplyRuleRepository     应用规则 Repository
     * @param barcodeTypeRepository          条码类型 Repository
     * @param barcodeRuleRepository          条码规则 Repository
     * @param barcodeRuleItemRepository      规则组成明细 Repository
     * @param barcodeTemplateRepository      条码模板 Repository
     * @param barcodeSerialRepository        流水记录 Repository
     * @param barcodePrintRecordRepository   打印记录 Repository
     * @param barcodeTemplateFieldRepository 模板字段 Repository，打印预览取字段
     * @param barcodeUseRecordRepository     使用记录 Repository，扫码轨迹查询
     * @param productRefRepository           产品只读引用 Repository
     * @param materialRefRepository          物料只读引用 Repository
     * @param workOrderRefRepository         工单只读引用 Repository，数据范围校验
     * @param barcodeSerialSequence          条码流水发号器
     * @param objectMapper                   JSON 序列化器，打印预览快照落库
     */
    public BarcodeInstanceServiceImpl(BarcodeRepository barcodeRepository,
                                      BarcodeApplyRuleRepository barcodeApplyRuleRepository,
                                      BarcodeTypeRepository barcodeTypeRepository,
                                      BarcodeRuleRepository barcodeRuleRepository,
                                      BarcodeRuleItemRepository barcodeRuleItemRepository,
                                      BarcodeTemplateRepository barcodeTemplateRepository,
                                      BarcodeSerialRepository barcodeSerialRepository,
                                      BarcodePrintRecordRepository barcodePrintRecordRepository,
                                      BarcodeTemplateFieldRepository barcodeTemplateFieldRepository,
                                      BarcodeUseRecordRepository barcodeUseRecordRepository,
                                      ProductRefRepository productRefRepository,
                                      MaterialRefRepository materialRefRepository,
                                      WorkOrderRefRepository workOrderRefRepository,
                                      BarcodeSerialSequence barcodeSerialSequence,
                                      ObjectMapper objectMapper) {
        this.barcodeRepository = barcodeRepository;
        this.barcodeApplyRuleRepository = barcodeApplyRuleRepository;
        this.barcodeTypeRepository = barcodeTypeRepository;
        this.barcodeRuleRepository = barcodeRuleRepository;
        this.barcodeRuleItemRepository = barcodeRuleItemRepository;
        this.barcodeTemplateRepository = barcodeTemplateRepository;
        this.barcodeSerialRepository = barcodeSerialRepository;
        this.barcodePrintRecordRepository = barcodePrintRecordRepository;
        this.barcodeTemplateFieldRepository = barcodeTemplateFieldRepository;
        this.barcodeUseRecordRepository = barcodeUseRecordRepository;
        this.productRefRepository = productRefRepository;
        this.materialRefRepository = materialRefRepository;
        this.workOrderRefRepository = workOrderRefRepository;
        this.barcodeSerialSequence = barcodeSerialSequence;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BarcodeGenerateRespVO generateBarcode(BarcodeGenerateReqVO reqVO) {
        // 前置校验与上下文装配只执行一次，实际取号、组合和落库由 generateOne 保持单一入口。
        GenerationContext context = prepareGeneration(reqVO);
        BarcodeEntity barcode = generateOne(context);
        logger.info("[生成条码] id: {}, barcodeValue: {}, applyRuleId: {}", barcode.getId(),
                barcode.getBarcodeValue(), reqVO.getApplyRuleId());
        return BarcodeInstanceConvert.toGenerateRespVO(barcode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<BarcodeGenerateRespVO> batchGenerateBarcodes(BarcodeBatchGenerateReqVO reqVO) {
        GenerationContext context = prepareGeneration(reqVO);
        // 同一传入值无法批量落库为多个唯一条码
        if (BarcodeSourceTypeEnum.INPUT_VALUE.getType().equals(context.applyRule().getSourceType())) {
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_BATCH_NOT_SUPPORT_INPUT_VALUE);
        }

        // 预分配结果容量，批量生成仍逐个取流水并写库，确保每个条码都经过唯一性校验。
        List<BarcodeGenerateRespVO> results = new java.util.ArrayList<>(reqVO.getQuantity());
        for (int i = 0; i < reqVO.getQuantity(); i++) {
            results.add(BarcodeInstanceConvert.toGenerateRespVO(generateOne(context)));
        }
        logger.info("[批量生成条码] applyRuleId: {}, quantity: {}", reqVO.getApplyRuleId(),
                reqVO.getQuantity());
        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public BarcodeParseRespVO parseBarcode(BarcodeParseReqVO reqVO) {
        // 条码值是数据库唯一业务键，解析时先从事实表定位唯一条码实体。
        BarcodeEntity barcode = barcodeRepository.findByBarcodeValueAndDeletedFalse(reqVO.getBarcodeValue())
                .orElseThrow(() -> new ServiceException(BarcodeErrorCodeConstants.BARCODE_NOT_EXISTS));

        // 类型及产品、物料档案只用于丰富解析结果；关联档案缺失时保留条码主数据而不伪造展示值。
        BarcodeTypeEntity barcodeType = barcodeTypeRepository
                .findByIdAndDeletedFalse(barcode.getBarcodeTypeId()).orElse(null);
        Map<Long, ProductRefEntity> productMap = barcode.getProductId() == null ? Map.of()
                : productRefRepository.findByIdAndDeletedFalse(barcode.getProductId())
                        .map(product -> Map.of(product.getId(), product)).orElse(Map.of());
        Map<Long, MaterialRefEntity> materialMap = barcode.getMaterialId() == null ? Map.of()
                : materialRefRepository.findByIdAndDeletedFalse(barcode.getMaterialId())
                        .map(material -> Map.of(material.getId(), material)).orElse(Map.of());
        return BarcodeInstanceConvert.toParseRespVO(barcode, barcodeType, productMap, materialMap);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelBarcode(Long id, BarcodeCancelReqVO reqVO) {
        // "已使用不可作废"由 CAS 前置状态=未使用 原子保证
        // 条件更新把“检查未使用”和“改为已作废”合并为一条 SQL，避免并发使用请求穿透先查后改窗口。
        int rows = barcodeRepository.updateStatus(id, BarcodeStatusEnum.UNUSED.getStatus(),
                BarcodeStatusEnum.CANCELLED.getStatus());
        if (rows == 0) {
            // 更新行数为零后回查实体，用不同业务错误区分已作废、已使用和记录不存在。
            BarcodeEntity barcode = validateBarcodeExists(id);
            if (BarcodeStatusEnum.CANCELLED.getStatus().equals(barcode.getBarcodeStatus())) {
                throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_ALREADY_CANCELLED);
            }
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_USED_NOT_CANCEL);
        }

        // 原因仅日志留痕：契约基线 barcode 表未设原因列，持久化须先登记数据库变更
        logger.info("[作废条码] id: {}, operator: {}, reason: {}", id,
                SecurityContextHolder.getRequiredLoginUserId(), reqVO.getReason());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BarcodePrintRespVO printBarcode(Long id, BarcodePrintReqVO reqVO) {
        // 作废条码不允许继续打印，模板必须从条码原应用规则解析，不能由客户端任意替换。
        BarcodeEntity barcode = validateBarcodeExists(id);
        if (BarcodeStatusEnum.CANCELLED.getStatus().equals(barcode.getBarcodeStatus())) {
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_CANCELLED_NOT_PRINT);
        }
        BarcodeTemplateEntity template = resolvePrintTemplate(barcode);

        // 打印序号 = 最近记录序号 + 1；重复打印必须填写原因(02-条码应用需求分析)
        int printCount = barcodePrintRecordRepository
                .findFirstByBarcodeIdAndDeletedFalseOrderByPrintCountDesc(id)
                .map(BarcodePrintRecordEntity::getPrintCount)
                .orElse(0) + 1;
        if (printCount > 1 && !StringUtils.hasText(reqVO.getReason())) {
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_REPRINT_REASON_REQUIRED);
        }

        // 预览数据与模板版本共同形成打印快照，使后续模板修改不影响历史打印记录复现。
        BarcodeTemplatePreviewRespVO preview = BarcodeTemplateConvert.toPreviewRespVO(template,
                barcodeTemplateFieldRepository.findByTemplateIdAndDeletedFalseOrderByIdAsc(template.getId()),
                barcode.getBarcodeValue(), buildPrintData(barcode));

        BarcodePrintRecordEntity record = new BarcodePrintRecordEntity();
        record.setBarcodeId(id);
        record.setTemplateId(template.getId());
        record.setTemplateVersion(template.getVersion());
        record.setPreviewContent(serializePreview(preview));
        record.setPrintBy(SecurityContextHolder.getRequiredLoginUserId());
        record.setPrintCount(printCount);
        record.setReprintReason(reqVO.getReason());
        record.setPrintTime(java.time.LocalDateTime.now());
        try {
            // 立即 flush 触发 barcode_id + print_count 唯一约束，防止两个并发重打得到相同序号。
            barcodePrintRecordRepository.saveAndFlush(record);
        } catch (DataIntegrityViolationException e) {
            // 并发重打撞唯一键 (barcode_id, print_count)，转业务错误由调用方重试
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_PRINT_CONFLICT);
        }

        logger.info("[打印条码] barcodeId: {}, printCount: {}, templateId: {}, reason: {}", id,
                printCount, template.getId(), reqVO.getReason());
        BarcodePrintRespVO respVO = new BarcodePrintRespVO();
        respVO.setPrintRecordId(record.getId());
        respVO.setBarcodeId(id);
        respVO.setBarcodeValue(barcode.getBarcodeValue());
        respVO.setTemplateId(template.getId());
        respVO.setTemplateVersion(template.getVersion());
        respVO.setPrintCount(printCount);
        respVO.setPrintTime(record.getPrintTime());
        respVO.setPreview(preview);
        return respVO;
    }

    @Override
    @Transactional(readOnly = true)
    public BarcodeInstanceRespVO getBarcodeInstance(Long id) {
        return BarcodeInstanceConvert.toRespVO(validateBarcodeExists(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<BarcodeInstanceRespVO> getBarcodeInstancePage(BarcodeInstancePageReqVO reqVO) {
        Specification<BarcodeEntity> specification = BarcodeSpecifications.page(reqVO);
        // 先统计匹配总数，无结果时直接返回空列表，避免执行后续分页查询。
        long total = barcodeRepository.count(specification);
        if (total == 0) {
            return PageResult.empty(reqVO.getPageNo(), reqVO.getPageSize());
        }

        int pageSize = reqVO.getPageSize();
        int totalPages = (int) ((total + pageSize - 1) / pageSize);
        // 超出末页的请求收敛到最后一页，避免返回空内容却携带不一致的分页元数据。
        int pageNo = Math.min(reqVO.getPageNo(), totalPages);
        PageRequest pageRequest = PageRequest.of(pageNo - 1, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        Page<BarcodeEntity> page = barcodeRepository.findAll(specification, pageRequest);
        return PageResult.of(BarcodeInstanceConvert.toRespVOList(page.getContent()), total, pageNo, pageSize);
    }

    /**
     * 生成上下文：一次校验，单个/批量生成共用。
     *
     * @param applyRule  启用的应用规则
     * @param rule       条码规则，规则生成来源时非空
     * @param segments   规则组成段，规则生成来源时非空
     * @param cycle      流水重置周期，规则生成来源时非空
     * @param scope      流水作用域，规则生成来源时非空
     * @param objectCode 对象编码(产品/物料编码)
     * @param reqVO      生成请求
     */
    private record GenerationContext(BarcodeApplyRuleEntity applyRule, BarcodeRuleEntity rule,
                                     List<RuleSegment> segments, BarcodeSerialResetCycleEnum cycle,
                                     String scope, String objectCode, BarcodeGenerateReqVO reqVO) {
    }

    /**
     * 生成前置校验与上下文装配：应用规则、类型、规则、模板、对象档案、
     * 工单范围逐项校验(实现方案 9.2 生成流程)。
     *
     * @param reqVO 生成请求
     * @return 生成上下文
     */
    private GenerationContext prepareGeneration(BarcodeGenerateReqVO reqVO) {
        // 应用规则是生成入口的聚合配置，必须存在、启用且来源类型允许在线生成。
        BarcodeApplyRuleEntity applyRule = barcodeApplyRuleRepository
                .findByIdAndDeletedFalse(reqVO.getApplyRuleId())
                .orElseThrow(() -> new ServiceException(
                        BarcodeErrorCodeConstants.BARCODE_APPLY_RULE_NOT_AVAILABLE));
        if (!CommonStatusEnum.ENABLED.getStatus().equals(applyRule.getStatus())) {
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_APPLY_RULE_NOT_AVAILABLE);
        }
        if (BarcodeSourceTypeEnum.EXTERNAL_IMPORT.getType().equals(applyRule.getSourceType())) {
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_APPLY_RULE_SOURCE_NOT_GENERATE);
        }

        // 类型、模板和对象档案分别校验，防止停用主数据继续产生新条码。
        validateBarcodeTypeAvailable(applyRule.getBarcodeTypeId());
        validateBarcodeTemplateAvailable(applyRule.getTemplateId());
        String objectCode = resolveObjectCode(applyRule);
        if (reqVO.getWorkOrderId() != null) {
            // 工单范围由当前登录用户和规则产品共同约束，不能只信任客户端提交的工单主键。
            validateWorkOrderInScope(reqVO.getWorkOrderId(), applyRule);
        }

        if (BarcodeSourceTypeEnum.INPUT_VALUE.getType().equals(applyRule.getSourceType())) {
            // 手工传值不需要规则段、重置周期和流水作用域，返回精简生成上下文。
            if (!StringUtils.hasText(reqVO.getInputBarcodeValue())) {
                throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_INPUT_VALUE_REQUIRED);
            }
            return new GenerationContext(applyRule, null, null, null, null, objectCode, reqVO);
        }

        // 规则生成来源：装配规则、组成段与流水作用域
        // 规则及其组成段来自 MySQL，组合前再次验证段顺序、变量和流水长度的整体合法性。
        BarcodeRuleEntity rule = barcodeRuleRepository.findByIdAndDeletedFalse(applyRule.getRuleId())
                .orElseThrow(() -> new ServiceException(BarcodeErrorCodeConstants.BARCODE_RULE_NOT_AVAILABLE));
        if (!CommonStatusEnum.ENABLED.getStatus().equals(rule.getStatus())) {
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_RULE_NOT_AVAILABLE);
        }
        List<RuleSegment> segments = barcodeRuleItemRepository
                .findByRuleIdAndDeletedFalseOrderBySeqAsc(rule.getId()).stream()
                .map(RuleSegment::from)
                .toList();
        List<String> errors = BarcodeValueComposer.validate(rule.getSerialLength(), segments);
        if (!errors.isEmpty()) {
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_RULE_CONFIG_INVALID,
                    String.join("；", errors));
        }
        BarcodeSerialResetCycleEnum cycle = BarcodeSerialResetCycleEnum.of(rule.getSerialResetCycle());
        if (cycle == null) {
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_RULE_CONFIG_INVALID,
                    "流水号重置周期取值不支持");
        }
        // 周期片段与对象编码共同隔离流水，确保不同产品/物料和不同重置周期互不抢占序号。
        String scope = cycle.scopeSegment(LocalDate.now()) + SCOPE_SEPARATOR + objectCode;
        if (scope.length() > SERIAL_SCOPE_MAX_LENGTH) {
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_SERIAL_SCOPE_TOO_LONG);
        }
        return new GenerationContext(applyRule, rule, segments, cycle, scope, objectCode, reqVO);
    }

    /**
     * 生成并落库单个条码。
     *
     * <p>规则生成：取号 → 组合 → 预检查重(命中则取下一号，至多重试 3 次) →
     * 落库；预检穿透的并发窗口由唯一索引兜底，返回生成冲突由调用方重试。
     * 落库成功后推进 MySQL 流水事实。
     *
     * @param context 生成上下文
     * @return 已落库的条码实体
     */
    private BarcodeEntity generateOne(GenerationContext context) {
        BarcodeApplyRuleEntity applyRule = context.applyRule();
        BarcodeGenerateReqVO reqVO = context.reqVO();

        if (BarcodeSourceTypeEnum.INPUT_VALUE.getType().equals(applyRule.getSourceType())) {
            // 手工值先做友好重复检查，最终仍由数据库唯一索引处理并发窗口。
            String inputValue = reqVO.getInputBarcodeValue();
            if (barcodeRepository.existsByBarcodeValueAndDeletedFalse(inputValue)) {
                throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_VALUE_DUPLICATE);
            }
            return insertBarcode(inputValue, context);
        }

        BarcodeRuleEntity rule = context.rule();
        for (int attempt = 0; attempt < GENERATE_MAX_RETRY; attempt++) {
            // Redis 只承担高并发取号；返回的流水必须组合成完整条码并通过 MySQL 唯一性验证。
            long serial = barcodeSerialSequence.next(rule.getId(), context.scope(), context.cycle());
            String barcodeValue = BarcodeValueComposer.compose(context.segments(),
                    new ComposeContext(LocalDate.now(), productCodeOf(context), reqVO.getLineCode(),
                            serial, rule.getSerialLength()));
            // 预检查重：Redis 播种竞态等确定性撞码换下一号重试，避免污染当前事务
            if (barcodeRepository.existsByBarcodeValueAndDeletedFalse(barcodeValue)) {
                logger.warn("[生成条码撞码重试] ruleId: {}, scope: {}, serial: {}", rule.getId(),
                        context.scope(), serial);
                continue;
            }
            BarcodeEntity barcode = insertBarcode(barcodeValue, context);
            // 只有条码主表成功落库后才推进 MySQL 流水事实，失败事务不会记录未实际使用的事实序号。
            advanceSerialRecord(rule.getId(), context.scope(), serial);
            return barcode;
        }
        throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_GENERATE_CONFLICT);
    }

    /**
     * 落库条码行，唯一索引 uk_barcode_value 兜底预检穿透的并发窗口。
     *
     * @param barcodeValue 条码值
     * @param context      生成上下文
     * @return 已落库的条码实体
     */
    private BarcodeEntity insertBarcode(String barcodeValue, GenerationContext context) {
        BarcodeApplyRuleEntity applyRule = context.applyRule();
        BarcodeGenerateReqVO reqVO = context.reqVO();

        // 应用规则决定类型、模式及产品/物料归属，请求只补充工单、任务和显式批次等运行时上下文。
        BarcodeEntity barcode = new BarcodeEntity();
        barcode.setBarcodeValue(barcodeValue);
        barcode.setBarcodeTypeId(applyRule.getBarcodeTypeId());
        barcode.setBarcodeMode(applyRule.getBarcodeMode());
        barcode.setApplyRuleId(applyRule.getId());
        barcode.setProductId(applyRule.getProductId());
        barcode.setMaterialId(applyRule.getMaterialId());
        barcode.setBatchNo(resolveBatchNo(barcodeValue, context));
        barcode.setWorkOrderId(reqVO.getWorkOrderId());
        barcode.setTaskId(reqVO.getTaskId());
        barcode.setSourceType(applyRule.getSourceType());
        barcode.setBarcodeStatus(BarcodeStatusEnum.UNUSED.getStatus());
        barcode.setCreateBy(SecurityContextHolder.getRequiredLoginUserId());
        try {
            // saveAndFlush 立即触发条码值唯一索引，确保冲突在当前服务调用内转换成业务异常。
            return barcodeRepository.saveAndFlush(barcode);
        } catch (DataIntegrityViolationException e) {
            // 预检与落库间隙的并发撞码：事务已污染，转业务错误由调用方重试
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_VALUE_DUPLICATE);
        }
    }

    /**
     * 解析批次号：显式提交优先；批次码模式缺省取条码值(一批一码，
     * 批次码即批次标识)；唯一码模式无缺省。
     *
     * @param barcodeValue 条码值
     * @param context      生成上下文
     * @return 批次号，可为 null
     */
    private String resolveBatchNo(String barcodeValue, GenerationContext context) {
        if (StringUtils.hasText(context.reqVO().getBatchNo())) {
            return context.reqVO().getBatchNo();
        }
        return BarcodeModeEnum.BATCH.getMode().equals(context.applyRule().getBarcodeMode())
                ? barcodeValue : null;
    }

    /**
     * 推进 MySQL 流水事实到本次流水号；维度记录不存在时插入。
     *
     * @param ruleId 规则主键
     * @param scope  流水作用域
     * @param serial 本次流水号
     */
    private void advanceSerialRecord(Long ruleId, String scope, long serial) {
        // 优先执行单条条件更新，仅当新流水更大时推进已有事实记录，避免并发请求把值回退。
        int rows = barcodeSerialRepository.advanceSerial(ruleId, scope, serial);
        if (rows > 0) {
            return;
        }

        // 首次使用该“规则 + 作用域”时插入事实行；并发首插冲突由唯一索引统一兜底。
        BarcodeSerialEntity serialRecord = new BarcodeSerialEntity();
        serialRecord.setRuleId(ruleId);
        serialRecord.setSerialScope(scope);
        serialRecord.setCurrentSerial(serial);
        try {
            barcodeSerialRepository.saveAndFlush(serialRecord);
        } catch (DataIntegrityViolationException e) {
            // 同维度首条记录并发插入撞唯一索引：事务已污染，按生成冲突由调用方重试
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_GENERATE_CONFLICT);
        }
    }

    /**
     * 解析对象编码并校验档案可用：产品对象取产品编码，物料对象取物料编码。
     *
     * @param applyRule 应用规则
     * @return 对象编码
     */
    private String resolveObjectCode(BarcodeApplyRuleEntity applyRule) {
        if (applyRule.getProductId() != null) {
            ProductRefEntity product = productRefRepository
                    .findByIdAndDeletedFalse(applyRule.getProductId())
                    .orElseThrow(() -> new ServiceException(BarcodeErrorCodeConstants.PRODUCT_NOT_AVAILABLE));
            if (!CommonStatusEnum.ENABLED.getStatus().equals(product.getStatus())) {
                throw new ServiceException(BarcodeErrorCodeConstants.PRODUCT_NOT_AVAILABLE);
            }
            return product.getProductCode();
        }

        MaterialRefEntity material = materialRefRepository
                .findByIdAndDeletedFalse(applyRule.getMaterialId())
                .orElseThrow(() -> new ServiceException(BarcodeErrorCodeConstants.MATERIAL_NOT_AVAILABLE));
        if (!CommonStatusEnum.ENABLED.getStatus().equals(material.getStatus())) {
            throw new ServiceException(BarcodeErrorCodeConstants.MATERIAL_NOT_AVAILABLE);
        }
        return material.getMaterialCode();
    }

    /**
     * 产品对象的编码作为 productCode 变量取值；物料对象无产品编码。
     *
     * @param context 生成上下文
     * @return 产品编码；物料对象返回 null(规则含产品变量时组合器报变量缺值)
     */
    private String productCodeOf(GenerationContext context) {
        return context.applyRule().getProductId() != null ? context.objectCode() : null;
    }

    /**
     * 校验关联工单存在、产品一致且在登录用户授权车间范围内。
     *
     * <p>数据范围规则(协作边界 4.5)：范围由服务端解析，请求参数只能缩小；
     * 越权与不存在统一按无可见数据处理，不泄露其他车间工单是否存在。
     *
     * @param workOrderId 工单主键
     * @param applyRule   应用规则，产品一致性校验
     */
    private void validateWorkOrderInScope(Long workOrderId, BarcodeApplyRuleEntity applyRule) {
        WorkOrderRefEntity workOrder = workOrderRefRepository.findByIdAndDeletedFalse(workOrderId)
                .orElseThrow(() -> new ServiceException(
                        BarcodeErrorCodeConstants.BARCODE_WORK_ORDER_NOT_AVAILABLE));

        LoginUser loginUser = SecurityContextHolder.getRequiredLoginUser();
        List<String> roleCodes = loginUser.getRoleCodes() == null ? List.of() : loginUser.getRoleCodes();
        if (!roleCodes.contains(RoleCodeConstants.ADMIN)) {
            // 非管理员按所属车间收敛；未配置车间的账号无可见工单
            if (loginUser.getWorkshopId() == null
                    || !loginUser.getWorkshopId().equals(workOrder.getWorkshopId())) {
                throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_WORK_ORDER_NOT_AVAILABLE);
            }
        }
        if (applyRule.getProductId() != null
                && !applyRule.getProductId().equals(workOrder.getProductId())) {
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_WORK_ORDER_PRODUCT_MISMATCH);
        }
    }

    /**
     * 校验条码类型存在且启用。
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
     * 校验条码模板存在且启用。
     *
     * @param templateId 模板主键
     */
    private void validateBarcodeTemplateAvailable(Long templateId) {
        BarcodeTemplateEntity template = barcodeTemplateRepository.findByIdAndDeletedFalse(templateId)
                .orElseThrow(() -> new ServiceException(
                        BarcodeErrorCodeConstants.BARCODE_TEMPLATE_NOT_AVAILABLE));
        if (!CommonStatusEnum.ENABLED.getStatus().equals(template.getStatus())) {
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_TEMPLATE_NOT_AVAILABLE);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BarcodeImportRespVO importBarcodes(BarcodeImportReqVO reqVO) {
        // 导入只接受明确标记为 EXTERNAL_IMPORT 的启用应用规则，避免绕过在线生成规则。
        BarcodeApplyRuleEntity applyRule = barcodeApplyRuleRepository
                .findByIdAndDeletedFalse(reqVO.getApplyRuleId())
                .orElseThrow(() -> new ServiceException(
                        BarcodeErrorCodeConstants.BARCODE_APPLY_RULE_NOT_AVAILABLE));
        if (!CommonStatusEnum.ENABLED.getStatus().equals(applyRule.getStatus())) {
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_APPLY_RULE_NOT_AVAILABLE);
        }
        if (!BarcodeSourceTypeEnum.EXTERNAL_IMPORT.getType().equals(applyRule.getSourceType())) {
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_APPLY_RULE_SOURCE_NOT_IMPORT);
        }
        validateBarcodeTypeAvailable(applyRule.getBarcodeTypeId());
        validateBarcodeTemplateAvailable(applyRule.getTemplateId());

        // 逐条校验重复性与工单范围：失败条目跳过并记录原因，不中断整批(部分成功)
        // Set 负责本批去重，失败列表保留原索引，便于客户端准确定位并修正原始数据。
        java.util.Set<String> batchValues = new java.util.HashSet<>();
        List<BarcodeImportRespVO.Failure> failures = new java.util.ArrayList<>();
        int successCount = 0;
        for (int index = 0; index < reqVO.getItems().size(); index++) {
            BarcodeImportReqVO.Item item = reqVO.getItems().get(index);
            String failReason = importOne(item, applyRule, batchValues);
            if (failReason == null) {
                successCount++;
                continue;
            }
            // 失败条目不写条码表，只把可读原因汇总到响应；其余合法条目继续导入。
            BarcodeImportRespVO.Failure failure = new BarcodeImportRespVO.Failure();
            failure.setIndex(index);
            failure.setBarcodeValue(item.getBarcodeValue());
            failure.setReason(failReason);
            failures.add(failure);
        }

        logger.info("[导入条码] applyRuleId: {}, total: {}, success: {}, fail: {}",
                reqVO.getApplyRuleId(), reqVO.getItems().size(), successCount, failures.size());
        BarcodeImportRespVO respVO = new BarcodeImportRespVO();
        respVO.setTotalCount(reqVO.getItems().size());
        respVO.setSuccessCount(successCount);
        respVO.setFailCount(failures.size());
        respVO.setFailures(failures);
        return respVO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BarcodeUseRecordRespVO> getBarcodeUseRecords(Long id) {
        validateBarcodeExists(id);
        return BarcodeInstanceConvert.toUseRecordRespVOList(
                barcodeUseRecordRepository.findByBarcodeIdAndDeletedFalseOrderByBusinessTimeDesc(id));
    }

    /**
     * 导入单条条码，返回失败原因；成功返回 null。
     *
     * <p>批内重复与库内重复分别校验；预检与落库间隙的并发撞码由唯一索引
     * 兜底并中断整批(极小概率，客户端整批重试)。
     *
     * @param item        导入明细
     * @param applyRule   外部导入来源的应用规则
     * @param batchValues 本批已接受的条码值
     * @return 失败原因；成功返回 null
     */
    private String importOne(BarcodeImportReqVO.Item item, BarcodeApplyRuleEntity applyRule,
                             java.util.Set<String> batchValues) {
        // 先检查批内重复，再访问数据库检查历史条码，减少无意义的数据库查询。
        if (!batchValues.add(item.getBarcodeValue())) {
            return "与本批前序条目条码值重复";
        }
        if (barcodeRepository.existsByBarcodeValueAndDeletedFalse(item.getBarcodeValue())) {
            batchValues.remove(item.getBarcodeValue());
            return "条码值已存在";
        }
        if (item.getWorkOrderId() != null) {
            try {
                // 工单不存在、越权或产品不一致均作为该明细失败，不影响同批其他条目。
                validateWorkOrderInScope(item.getWorkOrderId(), applyRule);
            } catch (ServiceException e) {
                batchValues.remove(item.getBarcodeValue());
                return e.getMessage();
            }
        }

        // 导入条码沿用规则绑定的类型、模式和对象归属，但来源固定记录为外部导入。
        BarcodeEntity barcode = new BarcodeEntity();
        barcode.setBarcodeValue(item.getBarcodeValue());
        barcode.setBarcodeTypeId(applyRule.getBarcodeTypeId());
        barcode.setBarcodeMode(applyRule.getBarcodeMode());
        barcode.setApplyRuleId(applyRule.getId());
        barcode.setProductId(applyRule.getProductId());
        barcode.setMaterialId(applyRule.getMaterialId());
        barcode.setBatchNo(StringUtils.hasText(item.getBatchNo()) ? item.getBatchNo()
                : (BarcodeModeEnum.BATCH.getMode().equals(applyRule.getBarcodeMode())
                        ? item.getBarcodeValue() : null));
        barcode.setWorkOrderId(item.getWorkOrderId());
        barcode.setTaskId(item.getTaskId());
        barcode.setSourceType(BarcodeSourceTypeEnum.EXTERNAL_IMPORT.getType());
        barcode.setBarcodeStatus(BarcodeStatusEnum.UNUSED.getStatus());
        barcode.setCreateBy(SecurityContextHolder.getRequiredLoginUserId());
        try {
            barcodeRepository.saveAndFlush(barcode);
        } catch (DataIntegrityViolationException e) {
            // 预检与落库间隙的并发撞码：事务已污染，转业务错误整批重试
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_VALUE_DUPLICATE);
        }
        return null;
    }

    /**
     * 解析打印模板：取条码来源应用规则绑定的模板并校验启用。
     *
     * @param barcode 条码实体
     * @return 启用的模板实体
     */
    private BarcodeTemplateEntity resolvePrintTemplate(BarcodeEntity barcode) {
        if (barcode.getApplyRuleId() == null) {
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_TEMPLATE_NOT_AVAILABLE);
        }
        BarcodeApplyRuleEntity applyRule = barcodeApplyRuleRepository
                .findByIdAndDeletedFalse(barcode.getApplyRuleId())
                .orElseThrow(() -> new ServiceException(
                        BarcodeErrorCodeConstants.BARCODE_TEMPLATE_NOT_AVAILABLE));
        BarcodeTemplateEntity template = barcodeTemplateRepository
                .findByIdAndDeletedFalse(applyRule.getTemplateId())
                .orElseThrow(() -> new ServiceException(
                        BarcodeErrorCodeConstants.BARCODE_TEMPLATE_NOT_AVAILABLE));
        if (!CommonStatusEnum.ENABLED.getStatus().equals(template.getStatus())) {
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_TEMPLATE_NOT_AVAILABLE);
        }
        return template;
    }

    /**
     * 装配打印预览数据：条码值、批次号、产品/物料档案与工单号。
     *
     * @param barcode 条码实体
     * @return 数据来源 -> 展示内容
     */
    private Map<String, String> buildPrintData(BarcodeEntity barcode) {
        // 条码值始终存在，其余展示字段按关联档案是否可读逐项补充，缺失档案不会阻断打印。
        Map<String, String> data = new java.util.HashMap<>();
        data.put("barcodeValue", barcode.getBarcodeValue());
        if (barcode.getBatchNo() != null) {
            data.put("batchNo", barcode.getBatchNo());
        }
        if (barcode.getProductId() != null) {
            productRefRepository.findByIdAndDeletedFalse(barcode.getProductId()).ifPresent(product -> {
                data.put("productCode", product.getProductCode());
                data.put("productName", product.getProductName());
            });
        }
        if (barcode.getMaterialId() != null) {
            materialRefRepository.findByIdAndDeletedFalse(barcode.getMaterialId()).ifPresent(material -> {
                data.put("materialCode", material.getMaterialCode());
                data.put("materialName", material.getMaterialName());
            });
        }
        if (barcode.getWorkOrderId() != null) {
            workOrderRefRepository.findByIdAndDeletedFalse(barcode.getWorkOrderId())
                    .ifPresent(workOrder -> data.put("workOrderNo", workOrder.getWorkOrderNo()));
        }
        return data;
    }

    /**
     * 序列化打印预览快照，供打印历史复现当时的标签内容。
     *
     * @param preview 预览数据
     * @return JSON 字符串
     */
    private String serializePreview(BarcodeTemplatePreviewRespVO preview) {
        try {
            return objectMapper.writeValueAsString(preview);
        } catch (JacksonException e) {
            logger.error("[打印快照序列化失败]", e);
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_PRINT_SNAPSHOT_ERROR);
        }
    }

    /**
     * 校验条码存在且未删除。
     *
     * @param id 条码主键
     * @return 条码实体
     */
    private BarcodeEntity validateBarcodeExists(Long id) {
        return barcodeRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException(BarcodeErrorCodeConstants.BARCODE_NOT_EXISTS));
    }
}
