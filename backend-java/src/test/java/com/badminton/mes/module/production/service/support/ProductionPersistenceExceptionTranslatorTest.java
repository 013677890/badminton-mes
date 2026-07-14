package com.badminton.mes.module.production.service.support;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.production.constants.ProductionErrorCodeConstants;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link ProductionPersistenceExceptionTranslator} 约束匹配测试。
 *
 * @author 张竹灏
 * @date 2026/07/12
 */
class ProductionPersistenceExceptionTranslatorTest {

    @Test
    @DisplayName("约束识别：忽略大小写并遍历完整异常链")
    void isConstraintViolationMatchesNestedCauseIgnoringCase() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "outer", new RuntimeException("Duplicate entry for UK_ACTIVE_PRODUCT_CODE"));

        assertThat(ProductionPersistenceExceptionTranslator.isConstraintViolation(
                exception, "uk_active_product_code")).isTrue();
        assertThat(ProductionPersistenceExceptionTranslator.isConstraintViolation(
                exception, "uk_active_material_code")).isFalse();
    }

    @Test
    @DisplayName("异常转换：命中约束时转换为指定业务错误")
    void translateUniqueConstraintConvertsKnownConstraint() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "uk_active_material_code");

        assertThatThrownBy(() -> ProductionPersistenceExceptionTranslator.translateUniqueConstraint(
                exception, "UK_ACTIVE_MATERIAL_CODE",
                ProductionErrorCodeConstants.MATERIAL_CODE_DUPLICATE))
                .isInstanceOfSatisfying(ServiceException.class, serviceException ->
                        assertThat(serviceException.getErrorCode()).isSameAs(
                                ProductionErrorCodeConstants.MATERIAL_CODE_DUPLICATE));
    }

    @Test
    @DisplayName("异常转换：未知约束保留原数据库异常")
    void translateUniqueConstraintKeepsUnknownConstraint() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "some_other_constraint");

        assertThatThrownBy(() -> ProductionPersistenceExceptionTranslator.translateUniqueConstraint(
                exception, "uk_active_product_code",
                ProductionErrorCodeConstants.PRODUCT_CODE_DUPLICATE))
                .isSameAs(exception);
    }
}
