package com.badminton.mes.module.barcode.service.impl;

import java.util.List;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.enums.CommonStatusEnum;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.barcode.constants.BarcodeErrorCodeConstants;
import com.badminton.mes.module.barcode.controller.vo.BarcodeTypePageReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeTypeRespVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeTypeSaveReqVO;
import com.badminton.mes.module.barcode.convert.BarcodeTypeConvert;
import com.badminton.mes.module.barcode.dal.entity.BarcodeTypeEntity;
import com.badminton.mes.module.barcode.dal.repository.BarcodeApplyRuleRepository;
import com.badminton.mes.module.barcode.dal.repository.BarcodeRuleRepository;
import com.badminton.mes.module.barcode.dal.repository.BarcodeTypeRepository;
import com.badminton.mes.module.barcode.dal.repository.BarcodeTypeSpecifications;
import com.badminton.mes.module.barcode.service.BarcodeTypeService;

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
 * 条码类型 Service 实现。
 *
 * <p>编码唯一性采用"应用层先查 + 数据库唯一索引 uk_type_code 兜底"；
 * 状态流转与删除使用带条件的 CAS 更新，消除先查后改的并发窗口。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Service
public class BarcodeTypeServiceImpl implements BarcodeTypeService {

    private static final Logger logger = LoggerFactory.getLogger(BarcodeTypeServiceImpl.class);

    private final BarcodeTypeRepository barcodeTypeRepository;

    private final BarcodeRuleRepository barcodeRuleRepository;

    private final BarcodeApplyRuleRepository barcodeApplyRuleRepository;

    /**
     * 构造器注入：依赖不可变、便于单测中直接 new 出被测对象。
     *
     * @param barcodeTypeRepository      条码类型 Repository
     * @param barcodeRuleRepository      条码规则 Repository，删除前引用校验
     * @param barcodeApplyRuleRepository 条码应用规则 Repository，删除前引用校验
     */
    public BarcodeTypeServiceImpl(BarcodeTypeRepository barcodeTypeRepository,
                                  BarcodeRuleRepository barcodeRuleRepository,
                                  BarcodeApplyRuleRepository barcodeApplyRuleRepository) {
        this.barcodeTypeRepository = barcodeTypeRepository;
        this.barcodeRuleRepository = barcodeRuleRepository;
        this.barcodeApplyRuleRepository = barcodeApplyRuleRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createBarcodeType(BarcodeTypeSaveReqVO reqVO) {
        // 应用层先查提前给出友好提示；并发窗口由唯一索引兜底(见 insert 的异常转换)
        if (barcodeTypeRepository.existsByTypeCodeAndDeletedFalse(reqVO.getTypeCode())) {
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_TYPE_CODE_DUPLICATE);
        }

        BarcodeTypeEntity barcodeType = BarcodeTypeConvert.toEntity(reqVO);
        // 创建时统一启用，状态变更必须通过独立启停接口执行，避免保存请求混入状态副作用。
        barcodeType.setStatus(CommonStatusEnum.ENABLED.getStatus());
        try {
            // 立即 flush 触发数据库编码唯一索引，处理并发创建穿透应用层预检的情况。
            barcodeTypeRepository.saveAndFlush(barcodeType);
        } catch (DataIntegrityViolationException e) {
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_TYPE_CODE_DUPLICATE);
        }

        logger.info("[创建条码类型] id: {}, typeCode: {}", barcodeType.getId(), barcodeType.getTypeCode());
        return barcodeType.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBarcodeType(Long id, BarcodeTypeSaveReqVO reqVO) {
        // 先区分不存在错误，再检查排除自身后的活动编码唯一性，给前端返回准确提示。
        validateBarcodeTypeExists(id);
        if (barcodeTypeRepository.existsByTypeCodeAndIdNotAndDeletedFalse(reqVO.getTypeCode(), id)) {
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_TYPE_CODE_DUPLICATE);
        }

        int rows;
        try {
            // 条件更新只修改未删除记录，类型状态不随基础信息修改而变化。
            rows = barcodeTypeRepository.updateInfo(id, reqVO.getTypeCode(), reqVO.getTypeName(),
                    reqVO.getApplyObject());
        } catch (DataIntegrityViolationException e) {
            // 查重与更新的间隙内其他事务占用了编码，由唯一索引兜底转业务错误
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_TYPE_CODE_DUPLICATE);
        }
        // CAS 未命中：校验与更新的间隙内类型被并发删除
        if (rows == 0) {
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_TYPE_NOT_EXISTS);
        }

        logger.info("[修改条码类型] id: {}, typeCode: {}", id, reqVO.getTypeCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableBarcodeType(Long id) {
        // 一条 CAS SQL 同时校验原状态并完成启用，避免先查后改造成并发覆盖。
        int rows = barcodeTypeRepository.updateStatus(id, CommonStatusEnum.DISABLED.getStatus(),
                CommonStatusEnum.ENABLED.getStatus());
        if (rows == 0) {
            // 先区分"不存在"再报"已启用"，给出精确提示(EXC-003 分门别类提示)
            validateBarcodeTypeExists(id);
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_TYPE_ALREADY_ENABLED);
        }

        logger.info("[启用条码类型] id: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableBarcodeType(Long id) {
        // 仅启用类型能够转为停用，未命中时回查实体以区分不存在和已停用。
        int rows = barcodeTypeRepository.updateStatus(id, CommonStatusEnum.ENABLED.getStatus(),
                CommonStatusEnum.DISABLED.getStatus());
        if (rows == 0) {
            validateBarcodeTypeExists(id);
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_TYPE_ALREADY_DISABLED);
        }

        logger.info("[停用条码类型] id: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBarcodeType(Long id) {
        validateBarcodeTypeExists(id);
        // 已被条码规则或应用规则使用的类型不允许删除(02-条码应用需求分析)
        boolean inUse = barcodeRuleRepository.existsByBarcodeTypeIdAndDeletedFalse(id)
                || barcodeApplyRuleRepository.existsByBarcodeTypeIdAndDeletedFalse(id);
        // 任一规则引用都说明类型已经参与条码配置，逻辑删除会破坏历史规则解释，因此直接拒绝。
        if (inUse) {
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_TYPE_IN_USE_NOT_DELETE);
        }

        // 逻辑删除，deleted = false 条件构成 CAS，防止并发重复删除
        int rows = barcodeTypeRepository.logicDeleteById(id);
        if (rows == 0) {
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_TYPE_NOT_EXISTS);
        }

        logger.info("[删除条码类型] id: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public BarcodeTypeRespVO getBarcodeType(Long id) {
        return BarcodeTypeConvert.toRespVO(validateBarcodeTypeExists(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<BarcodeTypeRespVO> getBarcodeTypePage(BarcodeTypePageReqVO reqVO) {
        Specification<BarcodeTypeEntity> specification = BarcodeTypeSpecifications.page(reqVO);
        // 先 count：总数为 0 直接返回空页，省一次列表查询(SQL-005)
        long total = barcodeTypeRepository.count(specification);
        if (total == 0) {
            return PageResult.empty(reqVO.getPageNo(), reqVO.getPageSize());
        }

        // 请求页码超过总页数时按最后一页返回(API-009)
        int pageSize = reqVO.getPageSize();
        int totalPages = (int) ((total + pageSize - 1) / pageSize);
        int pageNo = Math.min(reqVO.getPageNo(), totalPages);
        PageRequest pageRequest = PageRequest.of(pageNo - 1, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        Page<BarcodeTypeEntity> page = barcodeTypeRepository.findAll(specification, pageRequest);
        return PageResult.of(BarcodeTypeConvert.toRespVOList(page.getContent()), total, pageNo, pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BarcodeTypeRespVO> getEnabledBarcodeTypeOptions() {
        // 下拉选项只读取启用且未删除类型，并按业务编码排序以保持界面顺序稳定。
        return BarcodeTypeConvert.toRespVOList(
                barcodeTypeRepository.findByStatusAndDeletedFalseOrderByTypeCodeAsc(
                        CommonStatusEnum.ENABLED.getStatus()));
    }

    /**
     * 校验条码类型存在且未删除。
     *
     * @param id 类型主键
     * @return 类型实体
     */
    private BarcodeTypeEntity validateBarcodeTypeExists(Long id) {
        return barcodeTypeRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException(BarcodeErrorCodeConstants.BARCODE_TYPE_NOT_EXISTS));
    }
}
