package com.badminton.mes.module.scene.service;
import com.badminton.mes.module.scene.dal.entity.SceneCompletionOrderEntity;
/** 可替换的完工外部同步端口。 @author 刘涵 */
public interface CompletionSyncClient{void sync(SceneCompletionOrderEntity order,String targetSystem,String idempotencyKey);}
