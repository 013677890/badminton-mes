package com.badminton.mes.module.barcode.service.impl;

import java.util.List;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.enums.CommonStatusEnum;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.barcode.constants.BarcodeErrorCodeConstants;
import com.badminton.mes.module.barcode.controller.vo.BarcodeTemplateFieldSaveReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeTemplatePageReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeTemplatePreviewReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeTemplatePreviewRespVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeTemplateRespVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeTemplateSaveReqVO;
import com.badminton.mes.module.barcode.convert.BarcodeTemplateConvert;
import com.badminton.mes.module.barcode.dal.entity.BarcodeTemplateEntity;
import com.badminton.mes.module.barcode.dal.repository.BarcodeApplyRuleRepository;
import com.badminton.mes.module.barcode.dal.repository.BarcodeTemplateFieldRepository;
import com.badminton.mes.module.barcode.dal.repository.BarcodeTemplateRepository;
import com.badminton.mes.module.barcode.dal.repository.BarcodeTemplateSpecifications;
import com.badminton.mes.module.barcode.enums.BarcodeTemplateFieldTypeEnum;
import com.badminton.mes.module.barcode.service.BarcodeTemplateService;

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
 * 条码模板 Service 实现。
 *
 * <p>版本策略：同编码多版本各占一行；未被应用规则绑定时就地修改，
 * 已被绑定时保留原行并生成升版本新行，保证打印历史可回溯到当时的模板。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Service
public class BarcodeTemplateServiceImpl implements BarcodeTemplateService {

    private static final Logger logger = LoggerFactory.getLogger(BarcodeTemplateServiceImpl.class);

    /** 模板初始版本号 */
    private static final String INITIAL_VERSION = "V1";

    /** 系统管理的版本号前缀 */
    private static final String VERSION_PREFIX = "V";

    private final BarcodeTemplateRepository barcodeTemplateRepository;

    private final BarcodeTemplateFieldRepository barcodeTemplateFieldRepository;

    private final BarcodeApplyRuleRepository barcodeApplyRuleRepository;

    /**
     * 构造器注入：依赖不可变、便于单测中直接 new 出被测对象。
     *
     * @param barcodeTemplateRepository      条码模板 Repository
     * @param barcodeTemplateFieldRepository 模板字段 Repository
     * @param barcodeApplyRuleRepository     应用规则 Repository，绑定判定
     */
    public BarcodeTemplateServiceImpl(BarcodeTemplateRepository barcodeTemplateRepository,
                                      BarcodeTemplateFieldRepository barcodeTemplateFieldRepository,
                                      BarcodeApplyRuleRepository barcodeApplyRuleRepository) {
        this.barcodeTemplateRepository = barcodeTemplateRepository;
        this.barcodeTemplateFieldRepository = barcodeTemplateFieldRepository;
        this.barcodeApplyRuleRepository = barcodeApplyRuleRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createBarcodeTemplate(BarcodeTemplateSaveReqVO reqVO) {
        // 模板必须至少包含一个承载条码值的字段，否则即使保存成功也无法形成可打印标签。
        validateBarcodeValueField(reqVO.getFields());
        if (barcodeTemplateRepository.existsByTemplateCodeAndDeletedFalse(reqVO.getTemplateCode())) {
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_TEMPLATE_CODE_DUPLICATE);
        }

        // 新编码从 V1 开始且默认启用，版本和状态由服务端控制，客户端不能自行伪造。
        BarcodeTemplateEntity template = BarcodeTemplateConvert.toEntity(reqVO);
        template.setTemplateCode(reqVO.getTemplateCode());
        template.setVersion(INITIAL_VERSION);
        template.setStatus(CommonStatusEnum.ENABLED.getStatus());
        try {
            // 先 flush 主表以触发“编码 + 版本”唯一约束并取得模板主键，再保存字段明细。
            barcodeTemplateRepository.saveAndFlush(template);
        } catch (DataIntegrityViolationException e) {
            // 并发创建穿透应用层查重时，由唯一索引 uk_code_version 兜底转业务错误
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_TEMPLATE_CODE_DUPLICATE);
        }
        // 字段明细与模板主表同事务提交，防止出现有模板无字段或字段不完整的中间状态。
        barcodeTemplateFieldRepository.saveAll(
                BarcodeTemplateConvert.toFieldEntities(template.getId(), reqVO.getFields()));

        logger.info("[创建条码模板] id: {}, templateCode: {}", template.getId(), template.getTemplateCode());
        return template.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBarcodeTemplate(Long id, BarcodeTemplateSaveReqVO reqVO) {
        validateBarcodeValueField(reqVO.getFields());
        BarcodeTemplateEntity existing = validateBarcodeTemplateExists(id);

        // 已被应用规则绑定：保留原版本行，生成升版本新行(被绑定后修改需升版本)
        // 已投入使用的模板不能原地覆盖，否则历史打印记录引用的模板语义会被改写。
        if (barcodeApplyRuleRepository.existsByTemplateIdAndDeletedFalse(id)) {
            createNextVersion(existing, reqVO);
            return;
        }

        // 未被绑定：就地修改并整体重写字段，编码与版本不变
        // 未使用模板可以原地维护，Repository 条件更新确保并发删除后不会被重新写入。
        int rows = barcodeTemplateRepository.updateInfo(id, reqVO.getTemplateName(),
                reqVO.getPaperWidth(), reqVO.getPaperHeight());
        // CAS 未命中：校验与更新的间隙内模板被并发删除
        if (rows == 0) {
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_TEMPLATE_NOT_EXISTS);
        }
        // 字段采用整体替换，旧字段先逻辑删除，新字段在同一事务中完整写入。
        barcodeTemplateFieldRepository.logicDeleteByTemplateId(id);
        barcodeTemplateFieldRepository.saveAll(BarcodeTemplateConvert.toFieldEntities(id, reqVO.getFields()));
        logger.info("[就地修改条码模板] id: {}, version: {}", id, existing.getVersion());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableBarcodeTemplate(Long id) {
        // CAS 只允许从停用切换为启用，未命中后回查区分不存在和重复启用。
        int rows = barcodeTemplateRepository.updateStatus(id, CommonStatusEnum.DISABLED.getStatus(),
                CommonStatusEnum.ENABLED.getStatus());
        if (rows == 0) {
            validateBarcodeTemplateExists(id);
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_TEMPLATE_ALREADY_ENABLED);
        }

        logger.info("[启用条码模板] id: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableBarcodeTemplate(Long id) {
        // CAS 只允许从启用切换为停用，避免并发请求覆盖最新状态。
        int rows = barcodeTemplateRepository.updateStatus(id, CommonStatusEnum.ENABLED.getStatus(),
                CommonStatusEnum.DISABLED.getStatus());
        if (rows == 0) {
            validateBarcodeTemplateExists(id);
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_TEMPLATE_ALREADY_DISABLED);
        }

        logger.info("[停用条码模板] id: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public BarcodeTemplateRespVO getBarcodeTemplate(Long id) {
        BarcodeTemplateEntity template = validateBarcodeTemplateExists(id);
        return BarcodeTemplateConvert.toRespVO(template,
                barcodeTemplateFieldRepository.findByTemplateIdAndDeletedFalseOrderByIdAsc(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<BarcodeTemplateRespVO> getBarcodeTemplatePage(BarcodeTemplatePageReqVO reqVO) {
        Specification<BarcodeTemplateEntity> specification = BarcodeTemplateSpecifications.page(reqVO);
        // 无匹配模板时直接返回空分页，不执行内容查询。
        long total = barcodeTemplateRepository.count(specification);
        if (total == 0) {
            return PageResult.empty(reqVO.getPageNo(), reqVO.getPageSize());
        }

        int pageSize = reqVO.getPageSize();
        int totalPages = (int) ((total + pageSize - 1) / pageSize);
        int pageNo = Math.min(reqVO.getPageNo(), totalPages);
        PageRequest pageRequest = PageRequest.of(pageNo - 1, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        Page<BarcodeTemplateEntity> page = barcodeTemplateRepository.findAll(specification, pageRequest);
        return PageResult.of(BarcodeTemplateConvert.toRespVOList(page.getContent()), total, pageNo, pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public BarcodeTemplatePreviewRespVO previewBarcodeTemplate(BarcodeTemplatePreviewReqVO reqVO) {
        // 预览只读取持久化模板和字段并替换样例值，不写打印记录，也不改变模板版本。
        BarcodeTemplateEntity template = validateBarcodeTemplateExists(reqVO.getTemplateId());
        return BarcodeTemplateConvert.toPreviewRespVO(template,
                barcodeTemplateFieldRepository.findByTemplateIdAndDeletedFalseOrderByIdAsc(template.getId()),
                reqVO.getSampleBarcodeValue(), reqVO.getSampleData());
    }

    /**
     * 生成升版本的新模板行并保存新字段配置；原版本行与字段保持不变。
     *
     * @param existing 被绑定的现有版本行
     * @param reqVO    修改请求
     */
    private void createNextVersion(BarcodeTemplateEntity existing, BarcodeTemplateSaveReqVO reqVO) {
        // 新版本继承稳定模板编码，只递增系统版本号并采用请求中的新尺寸、名称及字段布局。
        BarcodeTemplateEntity nextVersion = BarcodeTemplateConvert.toEntity(reqVO);
        nextVersion.setTemplateCode(existing.getTemplateCode());
        nextVersion.setVersion(nextVersion(existing.getTemplateCode()));
        nextVersion.setStatus(CommonStatusEnum.ENABLED.getStatus());
        try {
            // 并发升版可能计算出同一版本号，数据库唯一索引负责最终仲裁。
            barcodeTemplateRepository.saveAndFlush(nextVersion);
        } catch (DataIntegrityViolationException e) {
            // 并发升版本撞号，由唯一索引 uk_code_version 兜底；按重复提交处理
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_TEMPLATE_CODE_DUPLICATE);
        }
        barcodeTemplateFieldRepository.saveAll(
                BarcodeTemplateConvert.toFieldEntities(nextVersion.getId(), reqVO.getFields()));
        logger.info("[条码模板升版本] templateCode: {}, {} -> {}, newId: {}", existing.getTemplateCode(),
                existing.getVersion(), nextVersion.getVersion(), nextVersion.getId());
    }

    /**
     * 计算同编码的下一版本号：取既有版本 V 后缀数字最大值 + 1。
     *
     * @param templateCode 模板编码
     * @return 下一版本号，如 V3
     */
    private String nextVersion(String templateCode) {
        // 只识别系统管理的 V+数字格式，异常历史版本不会参与最大值计算。
        int maxVersion = barcodeTemplateRepository.findByTemplateCodeAndDeletedFalse(templateCode).stream()
                .map(BarcodeTemplateEntity::getVersion)
                .filter(version -> version != null && version.matches("V\\d+"))
                .mapToInt(version -> Integer.parseInt(version.substring(VERSION_PREFIX.length())))
                .max()
                .orElse(0);
        return VERSION_PREFIX + (maxVersion + 1);
    }

    /**
     * 校验模板字段包含至少一个条码或二维码字段(02-条码应用需求分析)。
     *
     * @param fields 字段请求列表
     */
    private void validateBarcodeValueField(List<BarcodeTemplateFieldSaveReqVO> fields) {
        boolean hasBarcodeField = fields.stream()
                .anyMatch(field -> BarcodeTemplateFieldTypeEnum.carriesBarcodeValue(field.getFieldType()));
        if (!hasBarcodeField) {
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_TEMPLATE_MISSING_BARCODE_FIELD);
        }
    }

    /**
     * 校验条码模板存在且未删除。
     *
     * @param id 模板主键
     * @return 模板实体
     */
    private BarcodeTemplateEntity validateBarcodeTemplateExists(Long id) {
        return barcodeTemplateRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException(BarcodeErrorCodeConstants.BARCODE_TEMPLATE_NOT_EXISTS));
    }
}
