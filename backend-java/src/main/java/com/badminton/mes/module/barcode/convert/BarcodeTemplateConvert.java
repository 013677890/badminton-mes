package com.badminton.mes.module.barcode.convert;

import java.util.List;
import java.util.Map;

import com.badminton.mes.module.barcode.controller.vo.BarcodeTemplateFieldRespVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeTemplateFieldSaveReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeTemplatePreviewRespVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeTemplateRespVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeTemplateSaveReqVO;
import com.badminton.mes.module.barcode.dal.entity.BarcodeTemplateEntity;
import com.badminton.mes.module.barcode.dal.entity.BarcodeTemplateFieldEntity;
import com.badminton.mes.module.barcode.enums.BarcodeTemplateFieldTypeEnum;

import org.springframework.util.StringUtils;

/**
 * 条码模板 VO 与实体的转换器。
 *
 * <p>采用显式逐字段赋值：字段对应关系一目了然，编译期即可发现改名遗漏(MISC-002)。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
public final class BarcodeTemplateConvert {

    /**
     * 保存请求 VO 转模板实体；模板编码、版本与状态由 Service 按业务规则设置。
     *
     * @param reqVO 保存请求 VO
     * @return 模板实体
     */
    public static BarcodeTemplateEntity toEntity(BarcodeTemplateSaveReqVO reqVO) {
        BarcodeTemplateEntity template = new BarcodeTemplateEntity();
        template.setTemplateName(reqVO.getTemplateName());
        template.setPaperWidth(reqVO.getPaperWidth());
        template.setPaperHeight(reqVO.getPaperHeight());
        return template;
    }

    /**
     * 字段请求列表转字段实体列表。
     *
     * @param templateId 所属模板主键
     * @param fields     字段请求列表
     * @return 字段实体列表
     */
    public static List<BarcodeTemplateFieldEntity> toFieldEntities(
            Long templateId, List<BarcodeTemplateFieldSaveReqVO> fields) {
        return fields.stream().map(field -> {
            BarcodeTemplateFieldEntity entity = new BarcodeTemplateFieldEntity();
            entity.setTemplateId(templateId);
            entity.setFieldName(field.getFieldName());
            entity.setFieldType(field.getFieldType());
            entity.setDataSource(field.getDataSource());
            entity.setPosX(field.getPosX());
            entity.setPosY(field.getPosY());
            entity.setFontSize(field.getFontSize());
            return entity;
        }).toList();
    }

    /**
     * 模板实体转响应 VO，不含字段配置(分页列表使用)。
     *
     * @param template 模板实体
     * @return 响应 VO
     */
    public static BarcodeTemplateRespVO toRespVO(BarcodeTemplateEntity template) {
        BarcodeTemplateRespVO respVO = new BarcodeTemplateRespVO();
        respVO.setId(template.getId());
        respVO.setTemplateCode(template.getTemplateCode());
        respVO.setTemplateName(template.getTemplateName());
        respVO.setPaperWidth(template.getPaperWidth());
        respVO.setPaperHeight(template.getPaperHeight());
        respVO.setVersion(template.getVersion());
        respVO.setStatus(template.getStatus());
        respVO.setCreateTime(template.getCreateTime());
        respVO.setUpdateTime(template.getUpdateTime());
        return respVO;
    }

    /**
     * 模板实体与字段配置转详情响应 VO。
     *
     * @param template 模板实体
     * @param fields   字段实体列表
     * @return 详情响应 VO
     */
    public static BarcodeTemplateRespVO toRespVO(BarcodeTemplateEntity template,
                                                 List<BarcodeTemplateFieldEntity> fields) {
        BarcodeTemplateRespVO respVO = toRespVO(template);
        respVO.setFields(toFieldRespVOList(fields));
        return respVO;
    }

    /**
     * 模板实体列表转响应 VO 列表。
     *
     * @param list 模板实体列表
     * @return 响应 VO 列表；入参为空集合时返回空集合
     */
    public static List<BarcodeTemplateRespVO> toRespVOList(List<BarcodeTemplateEntity> list) {
        return list.stream().map(BarcodeTemplateConvert::toRespVO).toList();
    }

    /**
     * 字段实体列表转响应 VO 列表。
     *
     * @param fields 字段实体列表
     * @return 响应 VO 列表；入参为空集合时返回空集合
     */
    public static List<BarcodeTemplateFieldRespVO> toFieldRespVOList(
            List<BarcodeTemplateFieldEntity> fields) {
        return fields.stream().map(field -> {
            BarcodeTemplateFieldRespVO respVO = new BarcodeTemplateFieldRespVO();
            respVO.setId(field.getId());
            respVO.setFieldName(field.getFieldName());
            respVO.setFieldType(field.getFieldType());
            respVO.setDataSource(field.getDataSource());
            respVO.setPosX(field.getPosX());
            respVO.setPosY(field.getPosY());
            respVO.setFontSize(field.getFontSize());
            return respVO;
        }).toList();
    }

    /**
     * 模板与字段配置转打印预览响应：条码/二维码字段优先取样例条码值，
     * 其余按数据来源取样例数据；缺样例时展示内容为 null。
     *
     * @param template           模板实体
     * @param fields             字段实体列表
     * @param sampleBarcodeValue 样例条码值，可空
     * @param sampleData         样例数据(数据来源 -> 展示内容)，可空
     * @return 预览响应 VO
     */
    public static BarcodeTemplatePreviewRespVO toPreviewRespVO(BarcodeTemplateEntity template,
                                                               List<BarcodeTemplateFieldEntity> fields,
                                                               String sampleBarcodeValue,
                                                               Map<String, String> sampleData) {
        BarcodeTemplatePreviewRespVO respVO = new BarcodeTemplatePreviewRespVO();
        respVO.setTemplateId(template.getId());
        respVO.setTemplateCode(template.getTemplateCode());
        respVO.setVersion(template.getVersion());
        respVO.setPaperWidth(template.getPaperWidth());
        respVO.setPaperHeight(template.getPaperHeight());
        respVO.setFields(fields.stream().map(field -> {
            BarcodeTemplatePreviewRespVO.Field fieldVO = new BarcodeTemplatePreviewRespVO.Field();
            fieldVO.setFieldName(field.getFieldName());
            fieldVO.setFieldType(field.getFieldType());
            fieldVO.setDataSource(field.getDataSource());
            fieldVO.setPosX(field.getPosX());
            fieldVO.setPosY(field.getPosY());
            fieldVO.setFontSize(field.getFontSize());
            fieldVO.setSampleContent(resolveSampleContent(field, sampleBarcodeValue, sampleData));
            return fieldVO;
        }).toList());
        return respVO;
    }

    /**
     * 解析字段的样例展示内容。
     *
     * @param field              字段实体
     * @param sampleBarcodeValue 样例条码值
     * @param sampleData         样例数据
     * @return 展示内容；缺样例时为 null
     */
    private static String resolveSampleContent(BarcodeTemplateFieldEntity field,
                                               String sampleBarcodeValue,
                                               Map<String, String> sampleData) {
        if (BarcodeTemplateFieldTypeEnum.carriesBarcodeValue(field.getFieldType())
                && StringUtils.hasText(sampleBarcodeValue)) {
            return sampleBarcodeValue;
        }
        return sampleData == null ? null : sampleData.get(field.getDataSource());
    }

    private BarcodeTemplateConvert() {
    }
}
