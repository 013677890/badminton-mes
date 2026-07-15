package com.badminton.mes.module.barcode.convert;

import java.util.List;

import com.badminton.mes.module.barcode.controller.vo.BarcodeTypeRespVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeTypeSaveReqVO;
import com.badminton.mes.module.barcode.dal.entity.BarcodeTypeEntity;

/**
 * 条码类型 VO 与实体的转换器。
 *
 * <p>采用显式逐字段赋值：字段对应关系一目了然，编译期即可发现改名遗漏，
 * 也避免反射拷贝的性能损耗与浅拷贝陷阱(MISC-002 禁用 Apache BeanUtils)。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
public final class BarcodeTypeConvert {

    /**
     * 保存请求 VO 转实体，创建与修改共用；状态由 Service 按业务规则另行设置。
     *
     * @param reqVO 保存请求 VO
     * @return 类型实体
     */
    public static BarcodeTypeEntity toEntity(BarcodeTypeSaveReqVO reqVO) {
        BarcodeTypeEntity barcodeType = new BarcodeTypeEntity();
        barcodeType.setTypeCode(reqVO.getTypeCode());
        barcodeType.setTypeName(reqVO.getTypeName());
        barcodeType.setApplyObject(reqVO.getApplyObject());
        return barcodeType;
    }

    /**
     * 实体转响应 VO。
     *
     * @param barcodeType 类型实体
     * @return 响应 VO
     */
    public static BarcodeTypeRespVO toRespVO(BarcodeTypeEntity barcodeType) {
        BarcodeTypeRespVO respVO = new BarcodeTypeRespVO();
        respVO.setId(barcodeType.getId());
        respVO.setTypeCode(barcodeType.getTypeCode());
        respVO.setTypeName(barcodeType.getTypeName());
        respVO.setApplyObject(barcodeType.getApplyObject());
        respVO.setStatus(barcodeType.getStatus());
        respVO.setCreateTime(barcodeType.getCreateTime());
        respVO.setUpdateTime(barcodeType.getUpdateTime());
        return respVO;
    }

    /**
     * 实体列表转响应 VO 列表。
     *
     * @param list 类型实体列表
     * @return 响应 VO 列表；入参为空集合时返回空集合
     */
    public static List<BarcodeTypeRespVO> toRespVOList(List<BarcodeTypeEntity> list) {
        return list.stream().map(BarcodeTypeConvert::toRespVO).toList();
    }

    private BarcodeTypeConvert() {
    }
}
