package com.badminton.mes.module.scene.service.impl;

import java.time.LocalDateTime;
import java.util.*;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.enums.CommonStatusEnum;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.scene.constants.*;
import com.badminton.mes.module.scene.controller.vo.*;
import com.badminton.mes.module.scene.dal.entity.*;
import com.badminton.mes.module.scene.dal.repository.*;
import com.badminton.mes.module.scene.enums.SceneParameterValueTypeEnum;
import com.badminton.mes.module.scene.service.SceneProductionParameterService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import jakarta.persistence.criteria.Predicate;

/** 生产参数 Service 实现。 @author 刘涵 */
@Service
public class SceneProductionParameterServiceImpl implements SceneProductionParameterService {
    private final SceneProductionParameterRepository parameterRepository;
    private final SceneParameterChangeLogRepository logRepository;

    public SceneProductionParameterServiceImpl(SceneProductionParameterRepository parameterRepository,
                                               SceneParameterChangeLogRepository logRepository) {
        this.parameterRepository = parameterRepository; this.logRepository = logRepository;
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public Long createParameter(SceneProductionParameterSaveReqVO reqVO) {
        validateValue(reqVO.getValueType(), reqVO.getParamValue());
        if (parameterRepository.existsByParamCodeAndWorkshopIdAndLineIdAndProductIdAndDeletedFalse(
                reqVO.getParamCode(), reqVO.getWorkshopId(), reqVO.getLineId(), reqVO.getProductId())) {
            throw new ServiceException(SceneErrorCodeConstants.PARAM_DUPLICATE);
        }
        SceneProductionParameterEntity entity = toEntity(reqVO);
        entity.setStatus(CommonStatusEnum.ENABLED.getStatus());
        entity.setCreateBy(SecurityContextHolder.getRequiredLoginUserId());
        try { parameterRepository.saveAndFlush(entity); }
        catch (DataIntegrityViolationException exception) {
            throw new ServiceException(SceneErrorCodeConstants.PARAM_DUPLICATE);
        }
        insertLog(entity, null, entity.getParamValue(), null, entity.getStatus(), reqVO.getChangeReason());
        return entity.getId();
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public void updateParameter(Long id, SceneProductionParameterSaveReqVO reqVO) {
        validateValue(reqVO.getValueType(), reqVO.getParamValue());
        SceneProductionParameterEntity entity = requireParameter(id);
        String beforeValue = entity.getParamValue();
        Integer beforeStatus = entity.getStatus();
        entity.setParamCode(reqVO.getParamCode()); entity.setParamName(reqVO.getParamName());
        entity.setParamValue(reqVO.getParamValue()); entity.setValueType(reqVO.getValueType());
        entity.setWorkshopId(reqVO.getWorkshopId()); entity.setLineId(reqVO.getLineId());
        entity.setProductId(reqVO.getProductId()); entity.setRemark(reqVO.getRemark());
        try { parameterRepository.saveAndFlush(entity); }
        catch (DataIntegrityViolationException exception) {
            throw new ServiceException(SceneErrorCodeConstants.PARAM_DUPLICATE);
        }
        insertLog(entity, beforeValue, entity.getParamValue(), beforeStatus, entity.getStatus(), reqVO.getChangeReason());
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public void enableParameter(Long id, String reason) { changeStatus(id, CommonStatusEnum.ENABLED.getStatus(), reason); }
    @Override @Transactional(rollbackFor = Exception.class)
    public void disableParameter(Long id, String reason) { changeStatus(id, CommonStatusEnum.DISABLED.getStatus(), reason); }

    private void changeStatus(Long id, Integer status, String reason) {
        SceneProductionParameterEntity entity = requireParameter(id);
        Integer before = entity.getStatus(); entity.setStatus(status); parameterRepository.save(entity);
        insertLog(entity, entity.getParamValue(), entity.getParamValue(), before, status, reason);
    }

    @Override @Transactional(readOnly = true)
    public SceneProductionParameterRespVO getParameter(Long id) { return toResp(requireParameter(id)); }

    @Override @Transactional(readOnly = true)
    public PageResult<SceneProductionParameterRespVO> getParameterPage(SceneProductionParameterPageReqVO reqVO) {
        Specification<SceneProductionParameterEntity> spec = (root, query, cb) -> {
            List<Predicate> items = new ArrayList<>(); items.add(cb.isFalse(root.get("deleted")));
            if (StringUtils.hasText(reqVO.getParamCode())) items.add(cb.like(root.get("paramCode"), reqVO.getParamCode()+"%"));
            if (reqVO.getWorkshopId()!=null) items.add(cb.equal(root.get("workshopId"), reqVO.getWorkshopId()));
            if (reqVO.getLineId()!=null) items.add(cb.equal(root.get("lineId"), reqVO.getLineId()));
            if (reqVO.getProductId()!=null) items.add(cb.equal(root.get("productId"), reqVO.getProductId()));
            if (reqVO.getStatus()!=null) items.add(cb.equal(root.get("status"), reqVO.getStatus()));
            return cb.and(items.toArray(new Predicate[0]));
        };
        long total=parameterRepository.count(spec);
        if(total==0) return PageResult.empty(reqVO.getPageNo(),reqVO.getPageSize());
        int pages=(int)((total+reqVO.getPageSize()-1)/reqVO.getPageSize());
        int pageNo=Math.min(reqVO.getPageNo(),pages);
        Page<SceneProductionParameterEntity> page=parameterRepository.findAll(spec,
                PageRequest.of(pageNo-1,reqVO.getPageSize(),Sort.by(Sort.Direction.DESC,"id")));
        return PageResult.of(page.getContent().stream().map(this::toResp).toList(),total,pageNo,reqVO.getPageSize());
    }

    @Override @Transactional(readOnly = true)
    public SceneProductionParameterRespVO getEffectiveParameter(SceneEffectiveParameterReqVO reqVO) {
        return parameterRepository.findByParamCodeAndStatusAndDeletedFalse(
                        reqVO.getParamCode(), CommonStatusEnum.ENABLED.getStatus()).stream()
                .filter(item -> matches(item.getWorkshopId(), reqVO.getWorkshopId()))
                .filter(item -> matches(item.getLineId(), reqVO.getLineId()))
                .filter(item -> matches(item.getProductId(), reqVO.getProductId()))
                .max(Comparator.comparingInt(this::specificity)).map(this::toResp)
                .orElseGet(() -> defaultParameter(reqVO.getParamCode()));
    }

    @Override @Transactional(readOnly = true)
    public List<SceneParameterChangeLogRespVO> getChangeLogs(Long id) {
        requireParameter(id);
        return logRepository.findByParamIdAndDeletedFalseOrderByOperateTimeDescIdDesc(id).stream().map(log -> {
            SceneParameterChangeLogRespVO vo=new SceneParameterChangeLogRespVO();
            vo.setId(log.getId());vo.setBeforeValue(log.getBeforeValue());vo.setAfterValue(log.getAfterValue());
            vo.setBeforeStatus(log.getBeforeStatus());vo.setAfterStatus(log.getAfterStatus());
            vo.setChangeReason(log.getChangeReason());vo.setOperatorId(log.getOperatorId());vo.setOperateTime(log.getOperateTime());
            return vo;
        }).toList();
    }

    private void validateValue(Integer type,String value) {
        if (SceneParameterValueTypeEnum.SWITCH.getType().equals(type) && !Set.of("0","1").contains(value)
                || SceneParameterValueTypeEnum.QUANTITY.getType().equals(type) && !value.matches("\\d+")) {
            throw new ServiceException(SceneErrorCodeConstants.PARAM_VALUE_INVALID);
        }
    }
    private boolean matches(Long configured,Long requested){return configured==null||Objects.equals(configured,requested);}
    private int specificity(SceneProductionParameterEntity e){return(e.getWorkshopId()!=null?1:0)+(e.getLineId()!=null?2:0)+(e.getProductId()!=null?4:0);}
    private SceneProductionParameterEntity requireParameter(Long id){return parameterRepository.findByIdAndDeletedFalse(id).orElseThrow(()->new ServiceException(SceneErrorCodeConstants.PARAM_NOT_EXISTS));}
    private SceneProductionParameterEntity toEntity(SceneProductionParameterSaveReqVO r){SceneProductionParameterEntity e=new SceneProductionParameterEntity();e.setParamCode(r.getParamCode());e.setParamName(r.getParamName());e.setParamValue(r.getParamValue());e.setValueType(r.getValueType());e.setWorkshopId(r.getWorkshopId());e.setLineId(r.getLineId());e.setProductId(r.getProductId());e.setRemark(r.getRemark());return e;}
    private SceneProductionParameterRespVO toResp(SceneProductionParameterEntity e){SceneProductionParameterRespVO v=new SceneProductionParameterRespVO();v.setId(e.getId());v.setParamCode(e.getParamCode());v.setParamName(e.getParamName());v.setParamValue(e.getParamValue());v.setValueType(e.getValueType());v.setWorkshopId(e.getWorkshopId());v.setLineId(e.getLineId());v.setProductId(e.getProductId());v.setRemark(e.getRemark());v.setStatus(e.getStatus());v.setCreateTime(e.getCreateTime());v.setUpdateTime(e.getUpdateTime());return v;}
    private SceneProductionParameterRespVO defaultParameter(String code){SceneProductionParameterRespVO v=new SceneProductionParameterRespVO();v.setParamCode(code);v.setParamName(code);v.setParamValue(SceneParameterCodes.DEFAULT_VALUES.getOrDefault(code,"0"));v.setValueType(SceneParameterValueTypeEnum.SWITCH.getType());v.setStatus(CommonStatusEnum.ENABLED.getStatus());return v;}
    private void insertLog(SceneProductionParameterEntity e,String bv,String av,Integer bs,Integer as,String reason){SceneParameterChangeLogEntity log=new SceneParameterChangeLogEntity();log.setParamId(e.getId());log.setBeforeValue(bv);log.setAfterValue(av);log.setBeforeStatus(bs);log.setAfterStatus(as);log.setChangeReason(reason);log.setOperatorId(SecurityContextHolder.getRequiredLoginUserId());log.setOperateTime(LocalDateTime.now());logRepository.save(log);}
}
