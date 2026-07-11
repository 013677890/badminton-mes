package com.badminton.mes.module.equipment.service.impl;

import java.util.List;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.equipment.constants.EquipmentErrorCodeConstants;
import com.badminton.mes.module.equipment.controller.vo.EquipmentFaultPrinciplePageReqVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentFaultPrincipleRespVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentFaultPrincipleSaveReqVO;
import com.badminton.mes.module.equipment.convert.EquipmentFaultPrincipleConvert;
import com.badminton.mes.module.equipment.dal.entity.EquipmentFaultPrincipleEntity;
import com.badminton.mes.module.equipment.dal.repository.EquipmentCategoryRepository;
import com.badminton.mes.module.equipment.dal.repository.EquipmentFaultPrincipleRepository;
import com.badminton.mes.module.equipment.dal.repository.EquipmentFaultPrincipleSpecifications;
import com.badminton.mes.module.equipment.dal.repository.EquipmentRepairOrderRepository;
import com.badminton.mes.module.equipment.service.EquipmentFaultPrincipleService;

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
 * 设备故障原理 Service 实现。
 *
 * @author 角色C
 * @date 2026/07/10
 */
@Service
public class EquipmentFaultPrincipleServiceImpl implements EquipmentFaultPrincipleService {

    private static final Logger logger = LoggerFactory.getLogger(EquipmentFaultPrincipleServiceImpl.class);

    /** TODO(角色C, 2026/07/10): 认证模块建设后，改为从登录上下文获取当前用户 id */
    private static final Long DEFAULT_OPERATOR_ID = 1L;

    /** 新建故障原理默认启用 */
    private static final int DEFAULT_ENABLED_STATUS = 1;

    /** 新建故障原理默认低等级 */
    private static final String DEFAULT_FAULT_LEVEL = "LOW";

    /** 新建故障原理默认排序号 */
    private static final int DEFAULT_SORT_ORDER = 0;

    /** 故障编码数据库字段最大长度 */
    private static final int FAULT_CODE_MAX_LENGTH = 32;

    /** 逻辑删除编码后缀前缀，用于释放原故障编码唯一约束 */
    private static final String DELETED_CODE_SUFFIX_PREFIX = "_D";

    private final EquipmentFaultPrincipleRepository faultPrincipleRepository;

    private final EquipmentCategoryRepository categoryRepository;

    private final EquipmentRepairOrderRepository repairOrderRepository;

    /**
     * 构造器注入，保证依赖不可变。
     *
     * @param faultPrincipleRepository 设备故障原理 Repository
     * @param categoryRepository       设备类别 Repository
     * @param repairOrderRepository    设备报修任务 Repository
     */
    public EquipmentFaultPrincipleServiceImpl(EquipmentFaultPrincipleRepository faultPrincipleRepository,
                                              EquipmentCategoryRepository categoryRepository,
                                              EquipmentRepairOrderRepository repairOrderRepository) {
        this.faultPrincipleRepository = faultPrincipleRepository;
        this.categoryRepository = categoryRepository;
        this.repairOrderRepository = repairOrderRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createEquipmentFaultPrinciple(EquipmentFaultPrincipleSaveReqVO reqVO) {
        validateFaultCode(reqVO.getFaultCode(), null);
        validateCategoryAvailable(reqVO.getCategoryId());

        EquipmentFaultPrincipleEntity faultPrinciple = EquipmentFaultPrincipleConvert.toEntity(reqVO);
        faultPrinciple.setCreateBy(DEFAULT_OPERATOR_ID);
        if (faultPrinciple.getStatus() == null) {
            faultPrinciple.setStatus(DEFAULT_ENABLED_STATUS);
        }
        if (faultPrinciple.getFaultLevel() == null) {
            faultPrinciple.setFaultLevel(DEFAULT_FAULT_LEVEL);
        }
        if (faultPrinciple.getSortOrder() == null) {
            faultPrinciple.setSortOrder(DEFAULT_SORT_ORDER);
        }

        try {
            faultPrincipleRepository.saveAndFlush(faultPrinciple);
        } catch (DataIntegrityViolationException e) {
            if (e.getMessage() != null && e.getMessage().contains("uk_fault_code")) {
                throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_FAULT_PRINCIPLE_CODE_DUPLICATE);
            }
            throw e;
        }

        logger.info("[创建设备故障原理] id: {}, faultCode: {}", faultPrinciple.getId(), faultPrinciple.getFaultCode());
        return faultPrinciple.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEquipmentFaultPrinciple(Long id, EquipmentFaultPrincipleSaveReqVO reqVO) {
        EquipmentFaultPrincipleEntity existing = validateFaultPrincipleExists(id);
        validateFaultCode(reqVO.getFaultCode(), id);
        validateCategoryAvailable(reqVO.getCategoryId());

        existing.setFaultCode(reqVO.getFaultCode());
        existing.setFaultName(reqVO.getFaultName());
        existing.setCategoryId(reqVO.getCategoryId());
        existing.setFaultDescription(reqVO.getFaultDescription());
        existing.setSuggestedSolution(reqVO.getSuggestedSolution());
        existing.setRemark(reqVO.getRemark());
        if (reqVO.getFaultLevel() != null) {
            existing.setFaultLevel(reqVO.getFaultLevel());
        }
        if (reqVO.getSortOrder() != null) {
            existing.setSortOrder(reqVO.getSortOrder());
        }
        if (reqVO.getStatus() != null) {
            existing.setStatus(reqVO.getStatus());
        }

        faultPrincipleRepository.save(existing);
        logger.info("[修改设备故障原理] id: {}, faultCode: {}", id, reqVO.getFaultCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteEquipmentFaultPrinciple(Long id) {
        EquipmentFaultPrincipleEntity faultPrinciple = validateFaultPrincipleExists(id);
        long repairOrderCount = repairOrderRepository.countByFaultPrincipleIdAndDeletedFalse(id);
        if (repairOrderCount > 0) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_FAULT_PRINCIPLE_HAS_REPAIR_ORDER);
        }

        String deletedCode = buildDeletedFaultCode(faultPrinciple.getFaultCode(), faultPrinciple.getId());
        faultPrinciple.setFaultCode(deletedCode);
        faultPrinciple.setDeleted(true);
        faultPrincipleRepository.save(faultPrinciple);

        logger.info("[删除设备故障原理] id: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public EquipmentFaultPrincipleRespVO getEquipmentFaultPrinciple(Long id) {
        EquipmentFaultPrincipleEntity faultPrinciple = validateFaultPrincipleExists(id);
        return EquipmentFaultPrincipleConvert.toRespVO(faultPrinciple);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<EquipmentFaultPrincipleRespVO> getEquipmentFaultPrinciplePage(EquipmentFaultPrinciplePageReqVO reqVO) {
        Specification<EquipmentFaultPrincipleEntity> specification = EquipmentFaultPrincipleSpecifications.page(reqVO);

        long total = faultPrincipleRepository.count(specification);
        if (total == 0) {
            return PageResult.empty(reqVO.getPageNo(), reqVO.getPageSize());
        }

        int pageSize = reqVO.getPageSize();
        int totalPages = (int) ((total + pageSize - 1) / pageSize);
        int pageNo = Math.min(reqVO.getPageNo(), totalPages);

        PageRequest pageRequest = PageRequest.of(pageNo - 1, pageSize,
                Sort.by(Sort.Direction.ASC, "sortOrder").and(Sort.by(Sort.Direction.DESC, "id")));
        Page<EquipmentFaultPrincipleEntity> page = faultPrincipleRepository.findAll(specification, pageRequest);
        List<EquipmentFaultPrincipleEntity> list = page.getContent();

        return PageResult.of(EquipmentFaultPrincipleConvert.toRespVOList(list), total, pageNo, pageSize);
    }

    /**
     * 校验故障原理存在且未删除。
     *
     * @param id 故障原理主键
     * @return 故障原理实体
     */
    private EquipmentFaultPrincipleEntity validateFaultPrincipleExists(Long id) {
        return faultPrincipleRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_FAULT_PRINCIPLE_NOT_EXISTS));
    }

    /**
     * 校验故障编码唯一性。
     *
     * @param faultCode 故障编码
     * @param excludeId 排除的故障原理 id，创建时传 null
     */
    private void validateFaultCode(String faultCode, Long excludeId) {
        boolean exists = excludeId == null
                ? faultPrincipleRepository.existsByFaultCodeAndDeletedFalse(faultCode)
                : faultPrincipleRepository.existsByFaultCodeAndIdNotAndDeletedFalse(faultCode, excludeId);

        if (exists) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_FAULT_PRINCIPLE_CODE_DUPLICATE);
        }
    }

    /**
     * 校验设备类别存在且未删除。
     *
     * @param categoryId 设备类别 id，可空
     */
    private void validateCategoryAvailable(Long categoryId) {
        if (categoryId == null) {
            return;
        }

        boolean exists = categoryRepository.findByIdAndDeletedFalse(categoryId).isPresent();
        if (!exists) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_CATEGORY_NOT_EXISTS);
        }
    }

    /**
     * 构造逻辑删除后的故障编码。
     *
     * @param originalFaultCode 原故障编码
     * @param faultPrincipleId  故障原理主键
     * @return 长度不超过 32 的删除态故障编码
     */
    private String buildDeletedFaultCode(String originalFaultCode, Long faultPrincipleId) {
        String deletedCodeSuffix = DELETED_CODE_SUFFIX_PREFIX + Long.toString(faultPrincipleId, 36).toUpperCase();
        int preservedPrefixLength = FAULT_CODE_MAX_LENGTH - deletedCodeSuffix.length();
        if (preservedPrefixLength <= 0) {
            return deletedCodeSuffix.substring(0, FAULT_CODE_MAX_LENGTH);
        }

        String preservedOriginalCode = originalFaultCode.length() <= preservedPrefixLength
                ? originalFaultCode
                : originalFaultCode.substring(0, preservedPrefixLength);
        return preservedOriginalCode + deletedCodeSuffix;
    }
}
