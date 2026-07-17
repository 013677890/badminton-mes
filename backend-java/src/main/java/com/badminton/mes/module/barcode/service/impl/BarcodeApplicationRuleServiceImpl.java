package com.badminton.mes.module.barcode.service.impl;

import java.util.List;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.enums.CommonStatusEnum;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.barcode.constants.BarcodeErrorCodeConstants;
import com.badminton.mes.module.barcode.controller.vo.BarcodeApplicationRuleOptionReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeApplicationRulePageReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeApplicationRuleRespVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeApplicationRuleSaveReqVO;
import com.badminton.mes.module.barcode.convert.BarcodeApplicationRuleConvert;
import com.badminton.mes.module.barcode.dal.entity.BarcodeApplyRuleEntity;
import com.badminton.mes.module.barcode.dal.entity.BarcodeRuleEntity;
import com.badminton.mes.module.barcode.dal.entity.BarcodeTemplateEntity;
import com.badminton.mes.module.barcode.dal.entity.BarcodeTypeEntity;
import com.badminton.mes.module.barcode.dal.entity.MaterialRefEntity;
import com.badminton.mes.module.barcode.dal.entity.ProductRefEntity;
import com.badminton.mes.module.barcode.dal.repository.BarcodeApplyRuleRepository;
import com.badminton.mes.module.barcode.dal.repository.BarcodeApplyRuleSpecifications;
import com.badminton.mes.module.barcode.dal.repository.BarcodeRepository;
import com.badminton.mes.module.barcode.dal.repository.BarcodeRuleRepository;
import com.badminton.mes.module.barcode.dal.repository.BarcodeTemplateRepository;
import com.badminton.mes.module.barcode.dal.repository.BarcodeTypeRepository;
import com.badminton.mes.module.barcode.dal.repository.MaterialRefRepository;
import com.badminton.mes.module.barcode.dal.repository.ProductRefRepository;
import com.badminton.mes.module.barcode.enums.BarcodeApplyObjectTypeEnum;
import com.badminton.mes.module.barcode.enums.BarcodeSourceTypeEnum;
import com.badminton.mes.module.barcode.service.BarcodeApplicationRuleService;

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
 * 条码应用规则 Service 实现。
 *
 * <p>"同对象同类型仅一条启用默认规则"采用应用层预检 + 数据库生成列唯一索引
 * uk_active_default 兜底；启用前校验条码类型、条码规则和标签模板均处于启用状态。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Service
public class BarcodeApplicationRuleServiceImpl implements BarcodeApplicationRuleService {

    private static final Logger logger = LoggerFactory.getLogger(BarcodeApplicationRuleServiceImpl.class);

    private final BarcodeApplyRuleRepository barcodeApplyRuleRepository;

    private final BarcodeTypeRepository barcodeTypeRepository;

    private final BarcodeRuleRepository barcodeRuleRepository;

    private final BarcodeTemplateRepository barcodeTemplateRepository;

    private final BarcodeRepository barcodeRepository;

    private final ProductRefRepository productRefRepository;

    private final MaterialRefRepository materialRefRepository;

    /**
     * 构造器注入：依赖不可变、便于单测中直接 new 出被测对象。
     *
     * @param barcodeApplyRuleRepository 应用规则 Repository
     * @param barcodeTypeRepository      条码类型 Repository
     * @param barcodeRuleRepository      条码规则 Repository
     * @param barcodeTemplateRepository  条码模板 Repository
     * @param barcodeRepository          条码主表 Repository，删除前使用校验
     * @param productRefRepository       产品只读引用 Repository
     * @param materialRefRepository      物料只读引用 Repository
     */
    public BarcodeApplicationRuleServiceImpl(BarcodeApplyRuleRepository barcodeApplyRuleRepository,
                                             BarcodeTypeRepository barcodeTypeRepository,
                                             BarcodeRuleRepository barcodeRuleRepository,
                                             BarcodeTemplateRepository barcodeTemplateRepository,
                                             BarcodeRepository barcodeRepository,
                                             ProductRefRepository productRefRepository,
                                             MaterialRefRepository materialRefRepository) {
        this.barcodeApplyRuleRepository = barcodeApplyRuleRepository;
        this.barcodeTypeRepository = barcodeTypeRepository;
        this.barcodeRuleRepository = barcodeRuleRepository;
        this.barcodeTemplateRepository = barcodeTemplateRepository;
        this.barcodeRepository = barcodeRepository;
        this.productRefRepository = productRefRepository;
        this.materialRefRepository = materialRefRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createBarcodeApplicationRule(BarcodeApplicationRuleSaveReqVO reqVO) {
        // 先验证对象、来源以及类型/规则/模板引用，避免无效外键语义进入应用规则表。
        validateSaveRequest(reqVO);
        boolean defaultFlag = resolveDefaultFlag(reqVO);
        // 新规则默认启用，启用默认规则需预检唯一性；并发窗口由 uk_active_default 兜底
        if (defaultFlag) {
            validateNoActiveDefault(reqVO, null);
        }

        // 创建状态统一为启用；默认标记缺省值已经在服务层归一化为明确布尔值。
        BarcodeApplyRuleEntity applyRule = BarcodeApplicationRuleConvert.toEntity(reqVO);
        applyRule.setDefaultFlag(defaultFlag);
        applyRule.setStatus(CommonStatusEnum.ENABLED.getStatus());
        try {
            // 立即 flush 触发活动默认规则的生成列唯一索引，兜住并发创建窗口。
            barcodeApplyRuleRepository.saveAndFlush(applyRule);
        } catch (DataIntegrityViolationException e) {
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_APPLY_RULE_DEFAULT_DUPLICATE);
        }

        logger.info("[创建条码应用规则] id: {}, objectType: {}, barcodeTypeId: {}", applyRule.getId(),
                applyRule.getObjectType(), applyRule.getBarcodeTypeId());
        return applyRule.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBarcodeApplicationRule(Long id, BarcodeApplicationRuleSaveReqVO reqVO) {
        // 读取现有状态用于决定默认规则唯一性是否生效；更新接口本身不改变启停状态。
        BarcodeApplyRuleEntity existing = validateApplyRuleExists(id);
        validateSaveRequest(reqVO);
        boolean defaultFlag = resolveDefaultFlag(reqVO);
        // 修改不变更状态：当前为启用且默认时才需要唯一性预检(排除自身)
        if (defaultFlag && CommonStatusEnum.ENABLED.getStatus().equals(existing.getStatus())) {
            validateNoActiveDefault(reqVO, id);
        }

        int rows;
        try {
            // Repository 使用条件更新并只覆盖业务字段，状态和逻辑删除标记保持不变。
            rows = barcodeApplyRuleRepository.updateInfo(id, reqVO.getObjectType(), reqVO.getProductId(),
                    reqVO.getMaterialId(), reqVO.getBarcodeTypeId(), reqVO.getBarcodeMode(),
                    reqVO.getRuleId(), reqVO.getTemplateId(), reqVO.getSourceType(), defaultFlag);
        } catch (DataIntegrityViolationException e) {
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_APPLY_RULE_DEFAULT_DUPLICATE);
        }
        // CAS 未命中：校验与更新的间隙内应用规则被并发删除
        if (rows == 0) {
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_APPLY_RULE_NOT_EXISTS);
        }

        logger.info("[修改条码应用规则] id: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableBarcodeApplicationRule(Long id) {
        BarcodeApplyRuleEntity applyRule = validateApplyRuleExists(id);
        // 启用前校验条码类型、条码规则和标签模板均处于启用状态(接口契约)
        validateBarcodeTypeAvailable(applyRule.getBarcodeTypeId());
        if (applyRule.getRuleId() != null) {
            validateBarcodeRuleAvailable(applyRule.getRuleId(), applyRule.getBarcodeTypeId());
        }
        validateBarcodeTemplateAvailable(applyRule.getTemplateId());
        if (Boolean.TRUE.equals(applyRule.getDefaultFlag())) {
            // 默认规则按“对象 + 条码类型”检查，产品与物料统一折算为当前规则的对象主键。
            Long objectId = applyRule.getProductId() != null
                    ? applyRule.getProductId() : applyRule.getMaterialId();
            if (barcodeApplyRuleRepository.countActiveDefault(applyRule.getObjectType(), objectId,
                    applyRule.getBarcodeTypeId(), id, CommonStatusEnum.ENABLED.getStatus()) > 0) {
                throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_APPLY_RULE_DEFAULT_DUPLICATE);
            }
        }

        int rows;
        try {
            // CAS 只允许从停用切换到启用，重复请求不会产生无意义更新。
            rows = barcodeApplyRuleRepository.updateStatus(id, CommonStatusEnum.DISABLED.getStatus(),
                    CommonStatusEnum.ENABLED.getStatus());
        } catch (DataIntegrityViolationException e) {
            // 预检与启用的间隙内其他默认规则被并发启用，由 uk_active_default 兜底
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_APPLY_RULE_DEFAULT_DUPLICATE);
        }
        if (rows == 0) {
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_APPLY_RULE_ALREADY_ENABLED);
        }

        logger.info("[启用条码应用规则] id: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableBarcodeApplicationRule(Long id) {
        // 条件更新把当前状态判断与写入合并为原子操作，避免并发启停互相覆盖。
        int rows = barcodeApplyRuleRepository.updateStatus(id, CommonStatusEnum.ENABLED.getStatus(),
                CommonStatusEnum.DISABLED.getStatus());
        if (rows == 0) {
            validateApplyRuleExists(id);
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_APPLY_RULE_ALREADY_DISABLED);
        }

        logger.info("[停用条码应用规则] id: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBarcodeApplicationRule(Long id) {
        validateApplyRuleExists(id);
        // 接口契约"删除未使用应用规则"：已生成条码即视为已使用
        // 条码主表保留 apply_rule_id 历史来源，因此存在任何活动条码时必须阻止删除规则。
        if (barcodeRepository.existsByApplyRuleIdAndDeletedFalse(id)) {
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_APPLY_RULE_IN_USE_NOT_DELETE);
        }

        // Repository 通过条件逻辑删除避免并发重复删除，返回 0 表示记录已在检查后发生变化。
        int rows = barcodeApplyRuleRepository.logicDeleteById(id);
        if (rows == 0) {
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_APPLY_RULE_NOT_EXISTS);
        }

        logger.info("[删除条码应用规则] id: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public BarcodeApplicationRuleRespVO getBarcodeApplicationRule(Long id) {
        return BarcodeApplicationRuleConvert.toRespVO(validateApplyRuleExists(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<BarcodeApplicationRuleRespVO> getBarcodeApplicationRulePage(
            BarcodeApplicationRulePageReqVO reqVO) {
        Specification<BarcodeApplyRuleEntity> specification = BarcodeApplyRuleSpecifications.page(reqVO);
        // 总数为零时直接返回标准空分页，不再执行内容查询。
        long total = barcodeApplyRuleRepository.count(specification);
        if (total == 0) {
            return PageResult.empty(reqVO.getPageNo(), reqVO.getPageSize());
        }

        int pageSize = reqVO.getPageSize();
        int totalPages = (int) ((total + pageSize - 1) / pageSize);
        // 请求页码超过末页时返回最后一页，保证分页元信息与内容一致。
        int pageNo = Math.min(reqVO.getPageNo(), totalPages);
        PageRequest pageRequest = PageRequest.of(pageNo - 1, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        Page<BarcodeApplyRuleEntity> page = barcodeApplyRuleRepository.findAll(specification, pageRequest);
        return PageResult.of(BarcodeApplicationRuleConvert.toRespVOList(page.getContent()), total,
                pageNo, pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BarcodeApplicationRuleRespVO> getBarcodeApplicationRuleOptions(
            BarcodeApplicationRuleOptionReqVO reqVO) {
        return BarcodeApplicationRuleConvert.toRespVOList(
                barcodeApplyRuleRepository.findEnabledOptions(reqVO.getObjectType(), reqVO.getProductId(),
                        reqVO.getMaterialId(), reqVO.getBarcodeTypeId(),
                        CommonStatusEnum.ENABLED.getStatus()));
    }

    /**
     * 保存请求组合校验：对象匹配、来源规则必填、类型/规则/模板可用性。
     *
     * @param reqVO 保存请求
     */
    private void validateSaveRequest(BarcodeApplicationRuleSaveReqVO reqVO) {
        // 对象类型决定产品/物料字段的互斥关系，并同时验证对应主数据处于启用状态。
        validateObjectMatch(reqVO);
        if (BarcodeSourceTypeEnum.RULE_GENERATE.getType().equals(reqVO.getSourceType())
                && reqVO.getRuleId() == null) {
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_APPLY_RULE_RULE_REQUIRED);
        }
        // 停用类型不允许新建应用规则(02-条码应用需求分析)
        // 引用主数据逐项验证，避免新规则绑定已停用类型、规则或模板。
        validateBarcodeTypeAvailable(reqVO.getBarcodeTypeId());
        if (reqVO.getRuleId() != null) {
            validateBarcodeRuleAvailable(reqVO.getRuleId(), reqVO.getBarcodeTypeId());
        }
        validateBarcodeTemplateAvailable(reqVO.getTemplateId());
    }

    /**
     * 校验对象类型与产品/物料取值匹配，并校验对应档案可用。
     *
     * @param reqVO 保存请求
     */
    private void validateObjectMatch(BarcodeApplicationRuleSaveReqVO reqVO) {
        if (BarcodeApplyObjectTypeEnum.PRODUCT.getType().equals(reqVO.getObjectType())) {
            // 产品规则必须且只能提供 productId，防止同一规则同时归属产品和物料。
            if (reqVO.getProductId() == null || reqVO.getMaterialId() != null) {
                throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_APPLY_RULE_OBJECT_MISMATCH);
            }
            ProductRefEntity product = productRefRepository.findByIdAndDeletedFalse(reqVO.getProductId())
                    .orElseThrow(() -> new ServiceException(BarcodeErrorCodeConstants.PRODUCT_NOT_AVAILABLE));
            if (!CommonStatusEnum.ENABLED.getStatus().equals(product.getStatus())) {
                throw new ServiceException(BarcodeErrorCodeConstants.PRODUCT_NOT_AVAILABLE);
            }
            return;
        }

        // 非产品分支按物料规则处理，必须且只能提供 materialId。
        if (reqVO.getMaterialId() == null || reqVO.getProductId() != null) {
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_APPLY_RULE_OBJECT_MISMATCH);
        }
        MaterialRefEntity material = materialRefRepository.findByIdAndDeletedFalse(reqVO.getMaterialId())
                .orElseThrow(() -> new ServiceException(BarcodeErrorCodeConstants.MATERIAL_NOT_AVAILABLE));
        if (!CommonStatusEnum.ENABLED.getStatus().equals(material.getStatus())) {
            throw new ServiceException(BarcodeErrorCodeConstants.MATERIAL_NOT_AVAILABLE);
        }
    }

    /**
     * 解析默认规则标记：未提交时按数据库默认取 true。
     *
     * @param reqVO 保存请求
     * @return 默认规则标记
     */
    private boolean resolveDefaultFlag(BarcodeApplicationRuleSaveReqVO reqVO) {
        return reqVO.getDefaultFlag() == null || reqVO.getDefaultFlag();
    }

    /**
     * 预检"同对象同类型仅一条启用默认规则"。
     *
     * @param reqVO     保存请求
     * @param excludeId 排除的应用规则主键，创建时为 null
     */
    private void validateNoActiveDefault(BarcodeApplicationRuleSaveReqVO reqVO, Long excludeId) {
        // 产品和物料规则统一转换为对象主键后执行同一条数据库统计语义。
        Long objectId = reqVO.getProductId() != null ? reqVO.getProductId() : reqVO.getMaterialId();
        long count = barcodeApplyRuleRepository.countActiveDefault(reqVO.getObjectType(), objectId,
                reqVO.getBarcodeTypeId(), excludeId, CommonStatusEnum.ENABLED.getStatus());
        if (count > 0) {
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_APPLY_RULE_DEFAULT_DUPLICATE);
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
     * 校验条码规则存在、启用且适用于指定条码类型。
     *
     * @param ruleId        条码规则主键
     * @param barcodeTypeId 应用规则的条码类型主键
     */
    private void validateBarcodeRuleAvailable(Long ruleId, Long barcodeTypeId) {
        BarcodeRuleEntity rule = barcodeRuleRepository.findByIdAndDeletedFalse(ruleId)
                .orElseThrow(() -> new ServiceException(BarcodeErrorCodeConstants.BARCODE_RULE_NOT_AVAILABLE));
        if (!CommonStatusEnum.ENABLED.getStatus().equals(rule.getStatus())) {
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_RULE_NOT_AVAILABLE);
        }
        if (!rule.getBarcodeTypeId().equals(barcodeTypeId)) {
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_APPLY_RULE_TYPE_MISMATCH);
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

    /**
     * 校验应用规则存在且未删除。
     *
     * @param id 应用规则主键
     * @return 应用规则实体
     */
    private BarcodeApplyRuleEntity validateApplyRuleExists(Long id) {
        return barcodeApplyRuleRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException(
                        BarcodeErrorCodeConstants.BARCODE_APPLY_RULE_NOT_EXISTS));
    }
}
