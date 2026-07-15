package com.badminton.mes.module.barcode.convert;

import java.util.List;
import java.util.Map;

import com.badminton.mes.module.barcode.controller.vo.BarcodeGenerateRespVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeInstanceRespVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeParseRespVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeUseRecordRespVO;
import com.badminton.mes.module.barcode.dal.entity.BarcodeEntity;
import com.badminton.mes.module.barcode.dal.entity.BarcodeTypeEntity;
import com.badminton.mes.module.barcode.dal.entity.BarcodeUseRecordEntity;
import com.badminton.mes.module.barcode.dal.entity.MaterialRefEntity;
import com.badminton.mes.module.barcode.dal.entity.ProductRefEntity;

/**
 * 条码实例 VO 与实体的转换器。
 *
 * <p>采用显式逐字段赋值：字段对应关系一目了然，编译期即可发现改名遗漏(MISC-002)。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
public final class BarcodeInstanceConvert {

    /**
     * 实体转生成响应 VO。
     *
     * @param barcode 条码实体
     * @return 生成响应 VO
     */
    public static BarcodeGenerateRespVO toGenerateRespVO(BarcodeEntity barcode) {
        BarcodeGenerateRespVO respVO = new BarcodeGenerateRespVO();
        respVO.setId(barcode.getId());
        respVO.setBarcodeValue(barcode.getBarcodeValue());
        respVO.setBarcodeTypeId(barcode.getBarcodeTypeId());
        respVO.setBarcodeMode(barcode.getBarcodeMode());
        respVO.setBatchNo(barcode.getBatchNo());
        respVO.setSourceType(barcode.getSourceType());
        respVO.setBarcodeStatus(barcode.getBarcodeStatus());
        return respVO;
    }

    /**
     * 实体转实例响应 VO。
     *
     * @param barcode 条码实体
     * @return 实例响应 VO
     */
    public static BarcodeInstanceRespVO toRespVO(BarcodeEntity barcode) {
        BarcodeInstanceRespVO respVO = new BarcodeInstanceRespVO();
        respVO.setId(barcode.getId());
        respVO.setBarcodeValue(barcode.getBarcodeValue());
        respVO.setBarcodeTypeId(barcode.getBarcodeTypeId());
        respVO.setBarcodeMode(barcode.getBarcodeMode());
        respVO.setApplyRuleId(barcode.getApplyRuleId());
        respVO.setProductId(barcode.getProductId());
        respVO.setMaterialId(barcode.getMaterialId());
        respVO.setBatchNo(barcode.getBatchNo());
        respVO.setWorkOrderId(barcode.getWorkOrderId());
        respVO.setTaskId(barcode.getTaskId());
        respVO.setSourceType(barcode.getSourceType());
        respVO.setBarcodeStatus(barcode.getBarcodeStatus());
        respVO.setCreateBy(barcode.getCreateBy());
        respVO.setCreateTime(barcode.getCreateTime());
        respVO.setUpdateTime(barcode.getUpdateTime());
        return respVO;
    }

    /**
     * 实体列表转实例响应 VO 列表。
     *
     * @param list 条码实体列表
     * @return 响应 VO 列表；入参为空集合时返回空集合
     */
    public static List<BarcodeInstanceRespVO> toRespVOList(List<BarcodeEntity> list) {
        return list.stream().map(BarcodeInstanceConvert::toRespVO).toList();
    }

    /**
     * 实体与关联档案转解析响应 VO：附带类型、产品、物料业务上下文。
     *
     * @param barcode     条码实体
     * @param barcodeType 条码类型，可空(档案被删除时仅返回 id)
     * @param productMap  产品档案，key 为产品 id
     * @param materialMap 物料档案，key 为物料 id
     * @return 解析响应 VO
     */
    public static BarcodeParseRespVO toParseRespVO(BarcodeEntity barcode, BarcodeTypeEntity barcodeType,
                                                   Map<Long, ProductRefEntity> productMap,
                                                   Map<Long, MaterialRefEntity> materialMap) {
        BarcodeParseRespVO respVO = new BarcodeParseRespVO();
        respVO.setId(barcode.getId());
        respVO.setBarcodeValue(barcode.getBarcodeValue());
        respVO.setBarcodeTypeId(barcode.getBarcodeTypeId());
        if (barcodeType != null) {
            respVO.setBarcodeTypeCode(barcodeType.getTypeCode());
            respVO.setBarcodeTypeName(barcodeType.getTypeName());
        }
        respVO.setBarcodeMode(barcode.getBarcodeMode());
        respVO.setBatchNo(barcode.getBatchNo());
        respVO.setProductId(barcode.getProductId());
        // Map.of 空实现不接受 null key，先判空业务对象 id 再查档案
        ProductRefEntity product = barcode.getProductId() == null
                ? null : productMap.get(barcode.getProductId());
        if (product != null) {
            respVO.setProductCode(product.getProductCode());
            respVO.setProductName(product.getProductName());
        }
        respVO.setMaterialId(barcode.getMaterialId());
        MaterialRefEntity material = barcode.getMaterialId() == null
                ? null : materialMap.get(barcode.getMaterialId());
        if (material != null) {
            respVO.setMaterialCode(material.getMaterialCode());
            respVO.setMaterialName(material.getMaterialName());
        }
        respVO.setWorkOrderId(barcode.getWorkOrderId());
        respVO.setTaskId(barcode.getTaskId());
        respVO.setSourceType(barcode.getSourceType());
        respVO.setBarcodeStatus(barcode.getBarcodeStatus());
        respVO.setCreateTime(barcode.getCreateTime());
        return respVO;
    }

    /**
     * 使用记录实体列表转响应 VO 列表。
     *
     * @param records 使用记录实体列表
     * @return 响应 VO 列表；入参为空集合时返回空集合
     */
    public static List<BarcodeUseRecordRespVO> toUseRecordRespVOList(
            List<BarcodeUseRecordEntity> records) {
        return records.stream().map(record -> {
            BarcodeUseRecordRespVO respVO = new BarcodeUseRecordRespVO();
            respVO.setId(record.getId());
            respVO.setBarcodeId(record.getBarcodeId());
            respVO.setTaskId(record.getTaskId());
            respVO.setProcessId(record.getProcessId());
            respVO.setUserId(record.getUserId());
            respVO.setEquipmentId(record.getEquipmentId());
            respVO.setUseType(record.getUseType());
            respVO.setBusinessTime(record.getBusinessTime());
            respVO.setCreateTime(record.getCreateTime());
            return respVO;
        }).toList();
    }

    private BarcodeInstanceConvert() {
    }
}
