package com.badminton.mes.module.report.service.impl.kanban;

import static org.mockito.Mockito.*;
import com.badminton.mes.module.report.service.kanban.KanbanSnapshotService;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class KanbanPushServiceImplTest {
    @Test void shouldRefreshAndPushAllKnownScopes() {
        KanbanSnapshotService snapshots = mock(KanbanSnapshotService.class);
        SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);
        when(snapshots.knownScopes()).thenReturn(Set.of(new KanbanSnapshotService.Scope("line", 8L)));
        when(snapshots.refresh("line", 8L)).thenReturn(Map.of("version", 1L));
        new KanbanPushServiceImpl(snapshots, messaging).refreshAndPush();
        verify(messaging).convertAndSend(eq("/topic/report/kanban/line/8"), any(Object.class));
    }
}
