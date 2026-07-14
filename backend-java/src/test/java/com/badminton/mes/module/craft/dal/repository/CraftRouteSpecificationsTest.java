package com.badminton.mes.module.craft.dal.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link CraftRouteSpecifications} 单元测试。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
class CraftRouteSpecificationsTest {

    @Test
    @DisplayName("前缀查询：LIKE 通配符和转义字符按字面值转义")
    void escapeLikeMetacharacters() {
        assertThat(CraftRouteSpecifications.escapeLike("RT%_\\V1"))
                .isEqualTo("RT\\%\\_\\\\V1");
    }
}
