package com.badminton.mes.module.wage.service.support;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.wage.constants.WageErrorCodeConstants;

/** 计件工资金额与比例精确换算工具。 */
public final class WageAmountUtils {

    private static final BigDecimal AMOUNT_BASIS = BigDecimal.valueOf(10_000L);
    private static final BigDecimal RATE_BASIS = BigDecimal.valueOf(10_000L);

    /**
     * 将元转换为万分之一元整数。
     *
     * @param amount 金额，单位元
     * @return 金额，单位万分之一元
     */
    public static long toAmountBasis(BigDecimal amount) {
        try {
            return amount.multiply(AMOUNT_BASIS)
                    .setScale(0, RoundingMode.UNNECESSARY)
                    .longValueExact();
        } catch (ArithmeticException exception) {
            throw new ServiceException(WageErrorCodeConstants.AMOUNT_OUT_OF_RANGE);
        }
    }

    /** 将万分之一元整数转换为元。 */
    public static BigDecimal fromAmountBasis(Long amountBasis) {
        return amountBasis == null ? null : BigDecimal.valueOf(amountBasis, 4);
    }

    /**
     * 将数据库聚合后的万分之一元金额转换为元。
     *
     * @param amountBasis 金额合计，单位万分之一元
     * @return 金额合计，单位元
     */
    public static BigDecimal fromAmountBasis(BigDecimal amountBasis) {
        return amountBasis == null ? null : amountBasis.movePointLeft(4);
    }

    /** 将百分比转换为基点，100.00% 对应 10000。 */
    public static int toRateBasis(BigDecimal percentage) {
        return percentage.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).intValueExact();
    }

    /** 将基点转换为百分比。 */
    public static BigDecimal fromRateBasis(Integer rateBasis) {
        return rateBasis == null ? null : BigDecimal.valueOf(rateBasis, 2);
    }

    /**
     * 计算应付金额：合格数量金额减去不良扣减，最低为零。
     */
    public static long calculateAmount(BigDecimal qualifiedQuantity, BigDecimal defectQuantity,
                                       long unitPriceBasis, int deductionRateBasis) {
        try {
            BigDecimal qualifiedAmount = qualifiedQuantity.multiply(BigDecimal.valueOf(unitPriceBasis));
            BigDecimal deductionAmount = defectQuantity.multiply(BigDecimal.valueOf(unitPriceBasis))
                    .multiply(BigDecimal.valueOf(deductionRateBasis))
                    .divide(RATE_BASIS, 8, RoundingMode.HALF_UP);
            long calculatedAmount = qualifiedAmount.subtract(deductionAmount)
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValueExact();
            return Math.max(calculatedAmount, 0L);
        } catch (ArithmeticException exception) {
            throw new ServiceException(WageErrorCodeConstants.AMOUNT_OUT_OF_RANGE);
        }
    }

    private WageAmountUtils() {
    }
}
