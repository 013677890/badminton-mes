package com.badminton.mes.module.report.service.impl.kanban;

import com.badminton.mes.module.report.service.kanban.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/** 中控看板定时刷新与推送。 @author 刘涵 */
@Service
public class KanbanPushServiceImpl implements KanbanPushService {
    private final KanbanSnapshotService snapshotService;
    private final SimpMessagingTemplate messagingTemplate;
    public KanbanPushServiceImpl(KanbanSnapshotService snapshotService, SimpMessagingTemplate messagingTemplate) {
        this.snapshotService = snapshotService; this.messagingTemplate = messagingTemplate;
    }
    @Override @Scheduled(fixedDelayString = "${mes.report.kanban.refresh-interval:60000}")
    public void refreshAndPush() {
        snapshotService.get("central", null);
        for (KanbanSnapshotService.Scope scope : snapshotService.knownScopes()) {
            Object payload = snapshotService.refresh(scope.type(), scope.id());
            String scopeId = scope.id() == null ? "0" : scope.id().toString();
            messagingTemplate.convertAndSend("/topic/report/kanban/" + scope.type() + "/" + scopeId, payload);
            if (scope.id() != null) {
                messagingTemplate.convertAndSend("/topic/report/mini_app/realtime/"
                        + scope.type() + "/" + scopeId, payload);
            }
        }
    }
}
