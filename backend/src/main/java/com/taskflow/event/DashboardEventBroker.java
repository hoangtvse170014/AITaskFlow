package com.taskflow.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * In-process broker for dashboard refresh events. SSE controllers register a
 * listener per workspaceId; the AutonomousProjectCreationService (and any other
 * mutator) publishes events. This is intentionally simple (no STOMP broker) so
 * it works out of the box.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DashboardEventBroker {

    private final Map<UUID, List<Consumer<DashboardRefreshEvent>>> listenersByWorkspace = new ConcurrentHashMap<>();

    public void register(UUID workspaceId, Consumer<DashboardRefreshEvent> listener) {
        listenersByWorkspace.computeIfAbsent(workspaceId, k -> new CopyOnWriteArrayList<>()).add(listener);
        log.debug("Registered dashboard listener for workspace {}", workspaceId);
    }

    public void unregister(UUID workspaceId, Consumer<DashboardRefreshEvent> listener) {
        List<Consumer<DashboardRefreshEvent>> listeners = listenersByWorkspace.get(workspaceId);
        if (listeners != null) {
            listeners.remove(listener);
            if (listeners.isEmpty()) {
                listenersByWorkspace.remove(workspaceId);
            }
        }
        log.debug("Unregistered dashboard listener for workspace {}", workspaceId);
    }

    public void publish(DashboardRefreshEvent event) {
        List<Consumer<DashboardRefreshEvent>> listeners = listenersByWorkspace.get(event.getWorkspaceId());
        if (listeners == null || listeners.isEmpty()) return;
        log.info("Publishing dashboard refresh to {} listener(s) for workspace {}: {}",
                listeners.size(), event.getWorkspaceId(), event.getReason());
        for (Consumer<DashboardRefreshEvent> listener : listeners) {
            try {
                listener.accept(event);
            } catch (Exception ex) {
                log.warn("Dashboard listener threw: {}", ex.getMessage());
            }
        }
    }

    public int listenerCount(UUID workspaceId) {
        List<Consumer<DashboardRefreshEvent>> listeners = listenersByWorkspace.get(workspaceId);
        return listeners == null ? 0 : listeners.size();
    }
}