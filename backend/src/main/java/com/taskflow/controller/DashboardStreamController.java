package com.taskflow.controller;

import com.taskflow.entity.User;
import com.taskflow.event.DashboardEventBroker;
import com.taskflow.event.DashboardRefreshEvent;
import com.taskflow.exception.ResourceNotFoundException;
import com.taskflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Server-Sent Events stream for live dashboard refresh signals. Frontends
 * subscribe once per workspace:
 *   new EventSource('/api/dashboard/smart/stream?workspaceId=...')
 * The connection stays open and receives a JSON event whenever the
 * AutonomousProjectCreationService (or any other mutator) publishes a
 * DashboardRefreshEvent for that workspace.
 *
 * The stream also emits a periodic heartbeat so reverse proxies don't
 * time out the connection.
 */
@RestController
@RequestMapping("/api/dashboard/smart")
@RequiredArgsConstructor
@Slf4j
public class DashboardStreamController {

    private static final long HEARTBEAT_INTERVAL_SECONDS = 15;
    private static final long STREAM_TIMEOUT_MINUTES = 30;

    private final DashboardEventBroker broker;
    private final UserRepository userRepository;
    private final ScheduledExecutorService heartbeatExecutor =
            Executors.newScheduledThreadPool(2, r -> {
                Thread t = new Thread(r, "dashboard-heartbeat");
                t.setDaemon(true);
                return t;
            });

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam UUID workspaceId) {
        User currentUser = getCurrentUser();
        log.info("Opening dashboard SSE stream for workspace {} (user {})",
                workspaceId, currentUser.getId());

        SseEmitter emitter = new SseEmitter(TimeUnit.MINUTES.toMillis(STREAM_TIMEOUT_MINUTES));

        Consumer<DashboardRefreshEvent> listener = event -> sendEvent(emitter, event);
        broker.register(workspaceId, listener);

        // Send initial "connected" event so the frontend can confirm.
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(java.util.Map.of(
                            "workspaceId", workspaceId.toString(),
                            "userId", currentUser.getId().toString(),
                            "timestamp", java.time.LocalDateTime.now().toString())));
        } catch (IOException ex) {
            log.warn("Failed to send initial SSE event: {}", ex.getMessage());
        }

        // Heartbeat
        ScheduledExecutorService finalExec = heartbeatExecutor;
        var heartbeat = finalExec.scheduleAtFixedRate(() -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("heartbeat")
                        .data(java.util.Map.of(
                                "timestamp", java.time.LocalDateTime.now().toString(),
                                "workspaceId", workspaceId.toString())));
            } catch (Exception ex) {
                log.debug("Heartbeat failed for workspace {}: {}", workspaceId, ex.getMessage());
            }
        }, HEARTBEAT_INTERVAL_SECONDS, HEARTBEAT_INTERVAL_SECONDS, TimeUnit.SECONDS);

        Runnable cleanup = () -> {
            heartbeat.cancel(false);
            broker.unregister(workspaceId, listener);
            log.info("Closed dashboard SSE stream for workspace {} (user {})",
                    workspaceId, currentUser.getId());
        };
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(t -> cleanup.run());

        return emitter;
    }

    private void sendEvent(SseEmitter emitter, DashboardRefreshEvent event) {
        try {
            emitter.send(SseEmitter.event()
                    .name("refresh")
                    .data(java.util.Map.of(
                            "workspaceId", event.getWorkspaceId().toString(),
                            "reason", event.getReason(),
                            "refreshSignals", event.getRefreshSignals(),
                            "entityId", event.getEntityId() != null ? event.getEntityId().toString() : null,
                            "entityName", event.getEntityName(),
                            "timestamp", event.getOccurredAt().toString())));
        } catch (IOException ex) {
            log.debug("SSE send failed for workspace {}: {}", event.getWorkspaceId(), ex.getMessage());
            broker.unregister(event.getWorkspaceId(), this::noop);
        }
    }

    private void noop(DashboardRefreshEvent event) { /* placeholder for cleanup */ }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }
}