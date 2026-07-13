package com.badminton.mes.module.barcode.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.barcode.constants.BarcodeErrorCodeConstants;
import com.badminton.mes.module.barcode.controller.vo.BarcodeRuleItemSaveReqVO;
import com.badminton.mes.module.barcode.dal.entity.BarcodeRuleItemEntity;
import com.badminton.mes.module.barcode.enums.BarcodeRuleItemTypeEnum;
import com.badminton.mes.module.barcode.enums.BarcodeRuleVariableEnum;

import org.springframework.util.StringUtils;

/**
 * 条码值组合器：条码规则组成项的合法性校验与按上下文拼接生成。
 *
 * <p>规则预览/校验接口与条码实例生成共用同一套逻辑，保证"预览即所得"。
 * 纯函数实现，不访问数据库和 Redis；流水号只做格式化，取号由调用方负责。
 *
 * <p>格式上限按基线契约冻结(M1 待确认事项①)：生成条码总长不超过
 * barcode_value varchar(64)；流水号达到 10^位数-1 后报规则容量不足，不回绕。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
public final class BarcodeValueComposer {

    /** 条码值长度上限，与 barcode.barcode_value varchar(64) 一致 */
    public static final int BARCODE_VALUE_MAX_LENGTH = 64;

    /** 校验日期格式合法性用的固定样例日期 */
    private static final LocalDate SAMPLE_DATE = LocalDate.of(2026, 1, 31);

    /**
     * 规则组成段，屏蔽请求 VO 与实体的形态差异。
     *
     * @param seq        组成顺序
     * @param itemType   组成类型，取值见 {@link BarcodeRuleItemTypeEnum}
     * @param itemValue  常量值或变量名
     * @param dateFormat 日期格式
     * @param itemLength 该段长度，可空
     */
    public record RuleSegment(Integer seq, Integer itemType, String itemValue,
                              String dateFormat, Integer itemLength) {

        /**
         * 从保存/预览请求项构造。
         *
         * @param reqVO 组成项请求
         * @return 规则组成段
         */
        public static RuleSegment from(BarcodeRuleItemSaveReqVO reqVO) {
            return new RuleSegment(reqVO.getSeq(), reqVO.getItemType(), reqVO.getItemValue(),
                    reqVO.getDateFormat(), reqVO.getItemLength());
        }

        /**
         * 从规则明细实体构造。
         *
         * @param entity 组成项实体
         * @return 规则组成段
         */
        public static RuleSegment from(BarcodeRuleItemEntity entity) {
            return new RuleSegment(entity.getSeq(), entity.getItemType(), entity.getItemValue(),
                    entity.getDateFormat(), entity.getItemLength());
        }
    }

    /**
     * 条码组合上下文，携带拼接所需业务取值。
     *
     * @param date         业务日期，日期段格式化依据
     * @param productCode  产品编码，含产品编码变量时必须有值
     * @param lineCode     产线编码，含产线编码变量时必须有值
     * @param serial       本次流水号(已由调用方取号)
     * @param serialLength 流水号位数
     */
    public record ComposeContext(LocalDate date, String productCode, String lineCode,
                                 long serial, int serialLength) {
    }

    /**
     * 组合后的分段结果。
     *
     * @param seq      组成顺序
     * @param itemType 组成类型
     * @param content  该段内容
     */
    public record ComposedSegment(Integer seq, Integer itemType, String content) {
    }

    /**
     * 校验规则配置合法性，返回逐条错误说明。
     *
     * @param serialLength 流水号位数
     * @param segments     组成段列表
     * @return 错误列表；配置合法时为空集合
     */
    public static List<String> validate(int serialLength, List<RuleSegment> segments) {
        List<String> errors = new ArrayList<>();
        if (segments == null || segments.isEmpty()) {
            errors.add("规则组成明细不能为空");
            return errors;
        }

        Set<Integer> seqSet = new HashSet<>();
        int serialCount = 0;
        int knownLength = 0;
        for (RuleSegment segment : segments) {
            if (segment.seq() != null && !seqSet.add(segment.seq())) {
                errors.add("组成顺序 " + segment.seq() + " 重复");
            }
            BarcodeRuleItemTypeEnum itemType = BarcodeRuleItemTypeEnum.of(segment.itemType());
            if (itemType == null) {
                errors.add("组成顺序 " + segment.seq() + " 的组成类型不支持");
                continue;
            }
            switch (itemType) {
                case CONSTANT -> {
                    if (!StringUtils.hasText(segment.itemValue())) {
                        errors.add("组成顺序 " + segment.seq() + " 的常量值不能为空");
                    } else {
                        knownLength += segment.itemValue().length();
                    }
                }
                case DATE -> {
                    String dateError = validateDateFormat(segment);
                    if (dateError != null) {
                        errors.add(dateError);
                    } else {
                        knownLength += SAMPLE_DATE.format(
                                DateTimeFormatter.ofPattern(segment.dateFormat())).length();
                    }
                }
                case VARIABLE -> {
                    if (BarcodeRuleVariableEnum.of(segment.itemValue()) == null) {
                        errors.add("组成顺序 " + segment.seq() + " 的变量名不支持，仅支持 productCode/lineCode");
                    } else if (segment.itemLength() != null) {
                        knownLength += segment.itemLength();
                    }
                }
                case SERIAL -> {
                    serialCount++;
                    if (segment.itemLength() != null && segment.itemLength() != serialLength) {
                        errors.add("组成顺序 " + segment.seq() + " 的流水号段长度与规则流水位数不一致");
                    }
                    knownLength += serialLength;
                }
            }
        }
        if (serialCount != 1) {
            // 无流水号无法保证生成结果唯一，多个流水号语义不明(生成结果必须唯一)
            errors.add("规则必须且只能包含一个流水号组成项");
        }
        if (knownLength > BARCODE_VALUE_MAX_LENGTH) {
            errors.add("按已知段长估算条码长度超过上限 " + BARCODE_VALUE_MAX_LENGTH);
        }
        return errors;
    }

    /**
     * 按上下文逐段组合条码内容，并做流水容量与总长度硬校验。
     *
     * @param segments 组成段列表
     * @param context  组合上下文
     * @return 分段结果，按 seq 升序
     * @throws ServiceException 变量缺值(A0402)、流水超容量(A0420)或总长超限(A0420)
     */
    public static List<ComposedSegment> composeSegments(List<RuleSegment> segments, ComposeContext context) {
        List<ComposedSegment> composed = new ArrayList<>();
        int totalLength = 0;
        List<RuleSegment> ordered = segments.stream()
                .sorted(Comparator.comparing(RuleSegment::seq))
                .toList();
        for (RuleSegment segment : ordered) {
            String content = composeSegment(segment, context);
            totalLength += content.length();
            composed.add(new ComposedSegment(segment.seq(), segment.itemType(), content));
        }
        if (totalLength > BARCODE_VALUE_MAX_LENGTH) {
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_VALUE_TOO_LONG,
                    "生成条码长度 " + totalLength + " 超过上限 " + BARCODE_VALUE_MAX_LENGTH);
        }
        return composed;
    }

    /**
     * 按上下文组合完整条码值。
     *
     * @param segments 组成段列表
     * @param context  组合上下文
     * @return 条码值
     */
    public static String compose(List<RuleSegment> segments, ComposeContext context) {
        StringBuilder value = new StringBuilder();
        for (ComposedSegment segment : composeSegments(segments, context)) {
            value.append(segment.content());
        }
        return value.toString();
    }

    /**
     * 计算流水号容量：10^位数 - 1。
     *
     * @param serialLength 流水号位数
     * @return 单周期最大流水号
     */
    public static long serialCapacity(int serialLength) {
        long capacity = 1;
        for (int i = 0; i < serialLength; i++) {
            capacity *= 10;
        }
        return capacity - 1;
    }

    /**
     * 组合单个段内容。
     *
     * @param segment 组成段
     * @param context 组合上下文
     * @return 段内容
     */
    private static String composeSegment(RuleSegment segment, ComposeContext context) {
        BarcodeRuleItemTypeEnum itemType = BarcodeRuleItemTypeEnum.of(segment.itemType());
        if (itemType == null) {
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_RULE_CONFIG_INVALID,
                    "组成顺序 " + segment.seq() + " 的组成类型不支持");
        }
        return switch (itemType) {
            case CONSTANT -> segment.itemValue();
            case DATE -> {
                try {
                    yield context.date().format(DateTimeFormatter.ofPattern(segment.dateFormat()));
                } catch (RuntimeException e) {
                    throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_RULE_CONFIG_INVALID,
                            "组成顺序 " + segment.seq() + " 的日期格式不合法");
                }
            }
            case VARIABLE -> composeVariable(segment, context);
            case SERIAL -> composeSerial(context);
        };
    }

    /**
     * 组合变量段，变量缺值时报业务错误。
     *
     * @param segment 组成段
     * @param context 组合上下文
     * @return 变量取值
     */
    private static String composeVariable(RuleSegment segment, ComposeContext context) {
        BarcodeRuleVariableEnum variable = BarcodeRuleVariableEnum.of(segment.itemValue());
        String value = switch (variable) {
            case PRODUCT_CODE -> context.productCode();
            case LINE_CODE -> context.lineCode();
            case null -> null;
        };
        if (!StringUtils.hasText(value)) {
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_RULE_VARIABLE_MISSING,
                    "条码规则变量 " + segment.itemValue() + " 缺少取值");
        }
        return value;
    }

    /**
     * 组合流水号段：超出容量报规则容量不足(不回绕)，否则按位数左补零。
     *
     * @param context 组合上下文
     * @return 流水号段内容
     */
    private static String composeSerial(ComposeContext context) {
        long capacity = serialCapacity(context.serialLength());
        if (context.serial() > capacity || context.serial() < 1) {
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_RULE_SERIAL_CAPACITY_EXCEEDED,
                    "流水号 " + context.serial() + " 超出规则容量 " + capacity);
        }
        return String.format("%0" + context.serialLength() + "d", context.serial());
    }

    /**
     * 校验日期段格式：必填且能被 DateTimeFormatter 接受。
     *
     * @param segment 组成段
     * @return 错误说明；合法时返回 null
     */
    private static String validateDateFormat(RuleSegment segment) {
        if (!StringUtils.hasText(segment.dateFormat())) {
            return "组成顺序 " + segment.seq() + " 的日期格式不能为空";
        }
        try {
            SAMPLE_DATE.format(DateTimeFormatter.ofPattern(segment.dateFormat()));
            return null;
        } catch (RuntimeException e) {
            return "组成顺序 " + segment.seq() + " 的日期格式不合法";
        }
    }

    private BarcodeValueComposer() {
    }
}
