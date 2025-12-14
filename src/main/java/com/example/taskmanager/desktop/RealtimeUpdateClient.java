package com.example.taskmanager.desktop;

import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

public class RealtimeUpdateClient {

    private final WebSocketStompClient stompClient;
    private final String endpointUrl;
    private StompSession session;
    private final List<ProjectUpdateListener> projectListeners = new CopyOnWriteArrayList<>();
    private final List<TaskUpdateListener> taskListeners = new CopyOnWriteArrayList<>();

    public RealtimeUpdateClient(String baseUrl) {
        this.endpointUrl = baseUrl.replace("http", "ws") + "/ws";
        this.stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        this.stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    }

    public void connect() {
        try {
            ListenableFuture<StompSession> future = stompClient.connect(endpointUrl, new StompSessionHandlerAdapter() {
            });
            session = future.get(5, TimeUnit.SECONDS);
            subscribe();
        } catch (Exception ex) {
            System.err.println("Failed to connect to realtime endpoint: " + ex.getMessage());
        }
    }

    private void subscribe() {
        if (session == null || !session.isConnected()) {
            return;
        }
        session.subscribe("/topic/projects", new SimpleFrameHandler(ProjectEventMessage.class) {
            @Override
            protected void handlePayload(Object payload) {
                if (payload instanceof ProjectEventMessage) {
                    notifyProjectListeners((ProjectEventMessage) payload);
                }
            }
        });
        session.subscribe("/topic/tasks", new SimpleFrameHandler(TaskEventMessage.class) {
            @Override
            protected void handlePayload(Object payload) {
                if (payload instanceof TaskEventMessage) {
                    notifyTaskListeners((TaskEventMessage) payload);
                }
            }
        });
    }

    public void disconnect() {
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }

    public void addProjectListener(ProjectUpdateListener listener) {
        projectListeners.add(listener);
    }

    public void addTaskListener(TaskUpdateListener listener) {
        taskListeners.add(listener);
    }

    private void notifyProjectListeners(ProjectEventMessage message) {
        for (ProjectUpdateListener listener : projectListeners) {
            listener.onProjectEvent(message);
        }
    }

    private void notifyTaskListeners(TaskEventMessage message) {
        for (TaskUpdateListener listener : taskListeners) {
            listener.onTaskEvent(message);
        }
    }

    private abstract static class SimpleFrameHandler implements StompFrameHandler {
        private final Class<?> payloadType;

        protected SimpleFrameHandler(Class<?> payloadType) {
            this.payloadType = payloadType;
        }

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return payloadType;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            handlePayload(payload);
        }

        protected abstract void handlePayload(Object payload);
    }
}
