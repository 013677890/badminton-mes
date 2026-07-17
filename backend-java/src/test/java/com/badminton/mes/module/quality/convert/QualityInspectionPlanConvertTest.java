package com.badminton.mes.module.quality.convert;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.badminton.mes.module.quality.controller.vo.QualityInspectionPlanSaveReqVO;
import com.badminton.mes.module.quality.dal.entity.QualityInspectionItemEntity;
import com.badminton.mes.module.quality.dal.entity.QualityInspectionPlanEntity;
import com.badminton.mes.module.quality.dal.entity.QualityInspectionPlanItemEntity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 质量检验方案转换器单元测试。 */
class QualityInspectionPlanConvertTest {

    @Test
    void toEntityNormalizesNullDefaultFlagToFalse() {
        QualityInspectionPlanSaveReqVO request = new QualityInspectionPlanSaveReqVO();
        request.setPlanCode("PLAN-001");
        request.setPlanName("首件检验");
        request.setProductId(9L);
        request.setInspectionType("FIRST_ARTICLE");
        request.setEffectiveDate(LocalDate.of(2026, 7, 14));

        QualityInspectionPlanEntity entity = QualityInspectionPlanConvert.toEntity(request);

        assertThat(entity.getPlanCode()).isEqualTo("PLAN-001");
        assertThat(entity.getProductId()).isEqualTo(9L);
        assertThat(entity.getDefaultFlag()).isFalse();
    }

    @Test
    void responseJoinsPlanItemWithInspectionItemSnapshot() {
        QualityInspectionPlanEntity plan = new QualityInspectionPlanEntity();
        plan.setId(1L);
        plan.setPlanCode("PLAN-001");
        QualityInspectionPlanItemEntity relation = new QualityInspectionPlanItemEntity();
        relation.setId(2L);
        relation.setInspectionItemId(3L);
        relation.setSortOrder(1);
        QualityInspectionItemEntity item = new QualityInspectionItemEntity();
        item.setId(3L);
        item.setItemCode("WEIGHT");
        item.setItemName("重量");

        var response = QualityInspectionPlanConvert.toRespVO(
                plan, List.of(relation), Map.of(3L, item));

        assertThat(response.getItems()).singleElement().satisfies(result -> {
            assertThat(result.getInspectionItemId()).isEqualTo(3L);
            assertThat(result.getItemCode()).isEqualTo("WEIGHT");
            assertThat(result.getItemName()).isEqualTo("重量");
        });
    }
}
