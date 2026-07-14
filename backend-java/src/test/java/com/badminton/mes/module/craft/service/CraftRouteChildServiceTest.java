package com.badminton.mes.module.craft.service;

import java.util.List;

import com.badminton.mes.module.craft.controller.vo.CraftRouteStepSaveReqVO;
import com.badminton.mes.module.craft.dal.repository.CraftRouteDetailRepository;
import com.badminton.mes.module.craft.dal.repository.CraftRouteProductRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link CraftRouteChildService} 单元测试。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
@ExtendWith(MockitoExtension.class)
class CraftRouteChildServiceTest {

    @Mock
    private CraftRouteProductRepository productRepository;

    @Mock
    private CraftRouteDetailRepository detailRepository;

    private CraftRouteChildService childService;

    @BeforeEach
    void setUp() {
        childService = new CraftRouteChildService(productRepository, detailRepository);
        when(productRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(detailRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("首次插入子记录：不空跑逻辑删除")
    void createDoesNotDeleteNonexistentChildren() {
        childService.create(100L, List.of(10L), List.of(buildStep()), 9L);

        verify(detailRepository, never()).logicDeleteByRouteId(100L, 9L);
        verify(productRepository, never()).logicDeleteByRouteId(100L, 9L);
        verify(detailRepository).saveAll(anyList());
        verify(productRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("替换子记录：先逻辑删除再插入新集合")
    void replaceDeletesExistingChildren() {
        childService.replace(100L, List.of(10L), List.of(buildStep()), 9L);

        verify(detailRepository).logicDeleteByRouteId(100L, 9L);
        verify(productRepository).logicDeleteByRouteId(100L, 9L);
        verify(detailRepository).saveAll(anyList());
        verify(productRepository).saveAll(anyList());
    }

    /** 构造单步骤请求。 */
    private CraftRouteStepSaveReqVO buildStep() {
        CraftRouteStepSaveReqVO step = new CraftRouteStepSaveReqVO();
        step.setSequenceNo(1);
        step.setProcessId(20L);
        step.setInspectNode(false);
        return step;
    }
}
