package com.badminton.mes.module.andon.service.impl;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.andon.constants.AndonErrorCodeConstants;
import com.badminton.mes.module.andon.controller.vo.AndonReasonPageReqVO;
import com.badminton.mes.module.andon.controller.vo.AndonReasonRespVO;
import com.badminton.mes.module.andon.controller.vo.AndonReasonSaveReqVO;
import com.badminton.mes.module.andon.convert.AndonReasonConvert;
import com.badminton.mes.module.andon.dal.entity.AndonReasonEntity;
import com.badminton.mes.module.andon.dal.entity.AndonTypeEntity;
import com.badminton.mes.module.andon.dal.redis.AndonCache;
import com.badminton.mes.module.andon.dal.redis.AndonRedisKeyConstants;
import com.badminton.mes.module.andon.dal.repository.AndonReasonRepository;
import com.badminton.mes.module.andon.dal.repository.AndonReasonSpecifications;
import com.badminton.mes.module.andon.dal.repository.AndonTypeRepository;
import com.badminton.mes.module.andon.service.AndonReasonService;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 安灯异常原因维护实现。
 *
 * <p>原因始终归属于一个有效安灯类型。创建、跨类型更新和删除会先锁定所属类型；跨类型更新按类型
 * 主键升序加锁，避免两个并发请求反向迁移原因时形成死锁。已有事件引用的原因不得改变类型或删除，
 * 从而保持事件发起原因与实际原因的历史归属稳定。
 *
 * <p>删除采用编码改写加逻辑删除，释放原业务编码；更新、删除后的详情缓存仅在事务提交成功后失效。
 */
@Service
public class AndonReasonServiceImpl implements AndonReasonService {

    private static final int ENABLED = 1;
    private static final int DISABLED = 0;
    private static final Long DEFAULT_OPERATOR_ID = 1L;
    private static final String DELETED_CODE_PREFIX = "__DELETED_";

    private final AndonReasonRepository reasonRepository;
    private final AndonTypeRepository typeRepository;
    private final AndonCache andonCache;

    /** 注入原因仓储、所属类型仓储及详情缓存。 */
    public AndonReasonServiceImpl(
            AndonReasonRepository reasonRepository,
            AndonTypeRepository typeRepository,
            AndonCache andonCache) {
        this.reasonRepository = reasonRepository;
        this.typeRepository = typeRepository;
        this.andonCache = andonCache;
    }

    /** 锁定所属类型后创建原因，并在未指定启用状态时默认启用。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createReason(AndonReasonSaveReqVO request) {
        // 锁定所属类型后再检查原因编码并插入，防止原因写入期间类型被并发删除或改变状态。
        lockType(request.getAndonTypeId());
        validateReasonCodeUnique(request.getReasonCode(), null);

        // 审计字段和逻辑删除标记由服务层控制，避免客户端伪造数据库管理字段。
        AndonReasonEntity reason = AndonReasonConvert.toEntity(request);
        reason.setEnabledStatus(request.getEnabledStatus() == null ? ENABLED : request.getEnabledStatus());
        reason.setCreateBy(getCurrentOperatorId());
        reason.setDeleted(false);
        saveReason(reason);
        return reason.getId();
    }

    /**
     * 更新原因并保护历史归属；仅当类型发生变化且已有事件引用时拒绝修改。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateReason(Long id, AndonReasonSaveReqVO request) {
        AndonReasonEntity reasonSnapshot = getReasonEntity(id);
        // 类型迁移时按固定主键顺序锁定新旧类型，避免两个反向迁移事务互相等待。
        lockTypes(reasonSnapshot.getAndonTypeId(), request.getAndonTypeId());
        AndonReasonEntity reason = getReasonForUpdate(id);
        validateReasonCodeUnique(request.getReasonCode(), id);
        boolean changesReferencedReasonType = !reason.getAndonTypeId().equals(request.getAndonTypeId())
                && reasonRepository.countEventReferences(id) > 0;
        // 已被事件引用的原因可以改名称等展示字段，但不能改变其历史类型归属。
        if (changesReferencedReasonType) {
            throw new ServiceException(AndonErrorCodeConstants.REASON_HAS_EVENTS);
        }

        Integer previousEnabledStatus = reason.getEnabledStatus();
        AndonReasonConvert.copyEditableFields(request, reason);
        if (request.getEnabledStatus() == null) {
            reason.setEnabledStatus(previousEnabledStatus);
        }
        saveReason(reason);
        evictReasonCacheAfterCommit(id);
    }

    /** 删除前锁定所属类型并确认无事件引用，再改写编码、禁用并逻辑删除。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReason(Long id) {
        AndonReasonEntity reasonSnapshot = getReasonEntity(id);
        lockType(reasonSnapshot.getAndonTypeId());
        AndonReasonEntity reason = getReasonForUpdate(id);
        if (reasonRepository.countEventReferences(id) > 0) {
            throw new ServiceException(AndonErrorCodeConstants.REASON_HAS_EVENTS);
        }

        // 改写业务编码后再逻辑删除，使原编码可重新使用，同时保留历史记录的唯一标识。
        String deletedCode = DELETED_CODE_PREFIX + Long.toString(id, 36).toUpperCase();
        if (reasonRepository.existsByReasonCode(deletedCode)) {
            throw new ServiceException(AndonErrorCodeConstants.REASON_CODE_DUPLICATE);
        }
        reason.setReasonCode(deletedCode);
        reason.setEnabledStatus(DISABLED);
        reason.setDeleted(true);
        saveReason(reason);
        evictReasonCacheAfterCommit(id);
    }

    /** 读取原因详情并装配类型编码、名称；完整响应作为一个缓存单元保存。 */
    @Override
    @Transactional(readOnly = true)
    public AndonReasonRespVO getReason(Long id) {
        return andonCache.getOrLoadDetail(AndonRedisKeyConstants.REASON_RESOURCE,
                id, AndonReasonRespVO.class, () -> {
            AndonReasonEntity reason = getReasonEntity(id);
            AndonTypeEntity andonType = getTypeEntity(reason.getAndonTypeId());
            AndonReasonRespVO response = AndonReasonConvert.toRespVO(reason, andonType);
            return response;
        });
    }

    /** 分页查询原因，并批量加载所属类型以避免逐条关联查询。 */
    @Override
    @Transactional(readOnly = true)
    public PageResult<AndonReasonRespVO> getReasonPage(AndonReasonPageReqVO request) {
        var specification = AndonReasonSpecifications.page(request);
        // 先 count 再分页；总数为零时避免执行无意义的内容查询。
        long total = reasonRepository.count(specification);
        if (total == 0) {
            return PageResult.empty(request.getPageNo(), request.getPageSize());
        }

        int pageSize = request.getPageSize();
        int totalPages = (int) ((total + pageSize - 1) / pageSize);
        int pageNo = Math.min(request.getPageNo(), totalPages);
        PageRequest pageRequest = PageRequest.of(
                pageNo - 1,
                pageSize,
                Sort.by(Sort.Direction.DESC, "id"));
        Page<AndonReasonEntity> page = reasonRepository.findAll(specification, pageRequest);
        // 批量加载本页关联类型，避免原因列表产生逐行查询类型表的 N+1 问题。
        Map<Long, AndonTypeEntity> typesById = typeRepository.findAllById(
                        page.getContent().stream()
                                .map(AndonReasonEntity::getAndonTypeId)
                                .distinct()
                                .toList())
                .stream()
                .collect(Collectors.toMap(AndonTypeEntity::getId, Function.identity()));
        List<AndonReasonRespVO> list = page.getContent().stream()
                .map(reason -> AndonReasonConvert.toRespVO(
                        reason,
                        requireType(typesById, reason.getAndonTypeId())))
                .toList();
        return PageResult.of(list, total, pageNo, pageSize);
    }

    /** 校验未删除原因编码唯一；更新场景排除当前原因。 */
    private void validateReasonCodeUnique(String reasonCode, Long excludedId) {
        boolean exists = excludedId == null
                ? reasonRepository.existsByReasonCodeAndDeletedFalse(reasonCode)
                : reasonRepository.existsByReasonCodeAndIdNotAndDeletedFalse(reasonCode, excludedId);
        if (exists) {
            throw new ServiceException(AndonErrorCodeConstants.REASON_CODE_DUPLICATE);
        }
    }

    /** 通过统一实体读取语义验证所属类型存在。 */
    private void validateTypeExists(Long typeId) {
        getTypeEntity(typeId);
    }

    /**
     * 类型未变化时只加一次锁；发生迁移时按主键升序锁定新旧类型，建立稳定锁顺序。
     */
    private void lockTypes(Long firstTypeId, Long secondTypeId) {
        if (firstTypeId.equals(secondTypeId)) {
            lockType(firstTypeId);
            return;
        }
        Long lowerTypeId = Math.min(firstTypeId, secondTypeId);
        Long higherTypeId = Math.max(firstTypeId, secondTypeId);
        lockType(lowerTypeId);
        lockType(higherTypeId);
    }

    /** 锁定未删除类型，稳定原因写事务中的归属关系。 */
    private void lockType(Long typeId) {
        typeRepository.findByIdAndDeletedFalseForUpdate(typeId)
                .orElseThrow(() -> new ServiceException(AndonErrorCodeConstants.TYPE_NOT_EXISTS));
    }

    private AndonTypeEntity getTypeEntity(Long typeId) {
        return typeRepository.findByIdAndDeletedFalse(typeId)
                .orElseThrow(() -> new ServiceException(AndonErrorCodeConstants.TYPE_NOT_EXISTS));
    }

    /** 从批量预取结果中取得有效类型，拒绝输出关联信息不完整的原因。 */
    private AndonTypeEntity requireType(Map<Long, AndonTypeEntity> typesById, Long typeId) {
        AndonTypeEntity andonType = typesById.get(typeId);
        if (andonType == null || Boolean.TRUE.equals(andonType.getDeleted())) {
            throw new ServiceException(AndonErrorCodeConstants.TYPE_NOT_EXISTS);
        }
        return andonType;
    }

    private AndonReasonEntity getReasonEntity(Long id) {
        return reasonRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException(AndonErrorCodeConstants.REASON_NOT_EXISTS));
    }

    /** 以行锁读取原因，串行化同一原因的更新和删除。 */
    private AndonReasonEntity getReasonForUpdate(Long id) {
        return reasonRepository.findByIdAndDeletedFalseForUpdate(id)
                .orElseThrow(() -> new ServiceException(AndonErrorCodeConstants.REASON_NOT_EXISTS));
    }

    /** 刷新写入并将数据库并发唯一约束冲突转换为原因编码重复业务异常。 */
    private void saveReason(AndonReasonEntity reason) {
        try {
            // flush 让数据库唯一约束在当前调用点暴露，便于转换成前端可识别的业务错误码。
            reasonRepository.saveAndFlush(reason);
        } catch (DataIntegrityViolationException exception) {
            throw new ServiceException(AndonErrorCodeConstants.REASON_CODE_DUPLICATE);
        }
    }

    /** 在事务提交后失效原因详情，避免回滚事务提前删除有效缓存。 */
    private void evictReasonCacheAfterCommit(Long id) {
        andonCache.evictDetailAfterCommit(AndonRedisKeyConstants.REASON_RESOURCE, id);
    }

    /** 无登录上下文时使用系统默认操作者，兼容初始化和后台调用。 */
    private Long getCurrentOperatorId() {
        if (SecurityContextHolder.getLoginUser() == null) {
            return DEFAULT_OPERATOR_ID;
        }
        return SecurityContextHolder.getRequiredLoginUserId();
    }
}
