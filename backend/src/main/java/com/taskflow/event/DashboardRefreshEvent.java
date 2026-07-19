package com.taskflow.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Published whenever a workspace-scoped resource changes (e.g. project created,
 * tasks imported via AI). The Dashboard SSE controller listens for these and
 * pushes a refresh signal to connected dashboards.
 */
@Getter
public class DashboardRefreshEvent extends ApplicationEvent {

    private final UUID workspaceId;
    private final String reason;
    private final List<String> refreshSignals;
    private final UUID entityId;
    private final String entityName;
    private final LocalDateTime occurredAt;

    public DashboardRefreshEvent(Object source,
                                 UUID workspaceId,
                                 String reason,
                                 List<String> refreshSignals,
                                 UUID entityId,
                                 String entityName) {
        super(source);
        this.workspaceId = workspaceId;
        this.reason = reason;
        this.refreshSignals = refreshSignals;
        this.entityId = entityId;
        this.entityName = entityName;
        this.occurredAt = LocalDateTime.now();
    }
}