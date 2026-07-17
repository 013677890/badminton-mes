package com.badminton.mes.module.scene.service.impl;
import com.badminton.mes.module.scene.dal.entity.SceneCompletionOrderEntity;
import com.badminton.mes.module.scene.service.CompletionSyncClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
/** 未配置真实地址时显式失败，禁止伪造同步成功。 @author 刘涵 */
@Component
@ConditionalOnProperty(name = "mes.scene.completion-sync.url", havingValue = "", matchIfMissing = true)
public class UnconfiguredCompletionSyncClient implements CompletionSyncClient{
 @Override public void sync(SceneCompletionOrderEntity order,String targetSystem,String key){throw new IllegalStateException("未配置完工同步地址");}
}
