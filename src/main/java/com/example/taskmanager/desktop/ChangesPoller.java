package com.example.taskmanager.desktop;

import com.example.taskmanager.desktop.DesktopApiClient.ChangeEventDto;
import com.example.taskmanager.desktop.DesktopApiClient.ProjectDto;
import com.example.taskmanager.desktop.DesktopApiClient.TaskDto;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.SwingWorker;
import javax.swing.Timer;

/**
 * Polls /api/changes incrementally and applies deltas to the UI.
 */
public class ChangesPoller {

    private final DesktopApiClient apiClient;
    private final ProjectsListPanel projectsPanel;
    private final TasksListPanel tasksPanel;
    private final Timer timer;
    private final AtomicBoolean pollInProgress = new AtomicBoolean(false);
    private long lastEventId = 0L;

    public ChangesPoller(DesktopApiClient apiClient, ProjectsListPanel projectsPanel, TasksListPanel tasksPanel) {
        this.apiClient = apiClient;
        this.projectsPanel = projectsPanel;
        this.tasksPanel = tasksPanel;
        this.timer = new Timer(2000, e -> poll(false));
        this.timer.setRepeats(true);
    }

    public void reset() {
        lastEventId = 0L;
    }

    public void start() {
        if (!timer.isRunning()) {
            timer.start();
        }
    }

    public void stop() {
        if (timer.isRunning()) {
            timer.stop();
        }
    }

    public void poll(boolean showErrors) {
        if (apiClient.getCurrentUser() == null) {
            return;
        }
        if (!pollInProgress.compareAndSet(false, true)) {
            return;
        }
        final long since = lastEventId;
        System.out.println("[Poller] tick since=" + since);
        new SwingWorker<List<ChangeEventDto>, Void>() {
            @Override
            protected List<ChangeEventDto> doInBackground() {
                return apiClient.getChanges(since);
            }

            @Override
            protected void done() {
                pollInProgress.set(false);
                try {
                    List<ChangeEventDto> events = get();
                    long maxId = since;
                    for (ChangeEventDto e : events) {
                        if (e.getId() != null && e.getId() > maxId) {
                            maxId = e.getId();
                        }
                    }
                    System.out.println("[Poller] received " + events.size() + " events, maxId=" + maxId);
                    for (ChangeEventDto event : events) {
                        lastEventId = Math.max(lastEventId, event.getId() == null ? 0L : event.getId());
                        applyEvent(event);
                    }
                } catch (Exception ex) {
                    if (showErrors) {
                        System.err.println("Change poll failed: " + ex.getMessage());
                    }
                }
            }
        }.execute();
    }

    private void applyEvent(ChangeEventDto event) {
        if (event.getType() == null) {
            return;
        }
        System.out.println("[Poller] apply type=" + event.getType() + " entityId=" + event.getEntityId()
                + " projectId=" + event.getProjectId() + " currentProject=" + tasksPanel.getCurrentProjectId());
        String type = event.getType();
        if (type.startsWith("PROJECT_")) {
            handleProjectEvent(event);
        } else if (type.startsWith("TASK_")) {
            handleTaskEvent(event);
        }
    }

    private void handleProjectEvent(ChangeEventDto event) {
        Long projectId = event.getEntityId();
        if (projectId == null) {
            return;
        }
        switch (event.getType()) {
            case "PROJECT_CREATED":
            case "PROJECT_UPDATED":
                new SwingWorker<ProjectDto, Void>() {
                    @Override
                    protected ProjectDto doInBackground() {
                        return apiClient.getProjectById(projectId);
                    }

                    @Override
                    protected void done() {
                        try {
                            projectsPanel.upsertProject(get());
                        } catch (Exception ex) {
                            System.out.println("[Poller] project event failed type=" + event.getType()
                                    + " projectId=" + projectId + " err=" + ex);
                        }
                    }
                }.execute();
                break;
            case "PROJECT_DELETED":
                projectsPanel.removeProjectById(projectId);
                break;
            case "PROJECT_MEMBERS_UPDATED":
                projectsPanel.refreshMembers(projectId);
                break;
            default:
                break;
        }
    }

    private void handleTaskEvent(ChangeEventDto event) {
        Long taskId = event.getEntityId();
        Long projectId = event.getProjectId();
        if (taskId == null) {
            return;
        }
        Long currentProjectId = tasksPanel.getCurrentProjectId();
        if (currentProjectId == null || !currentProjectId.equals(projectId)) {
            return; // ignore unrelated project
        }
        switch (event.getType()) {
            case "TASK_CREATED":
            case "TASK_UPDATED":
                new SwingWorker<TaskDto, Void>() {
                    @Override
                    protected TaskDto doInBackground() {
                        return apiClient.getTaskById(taskId);
                    }

                    @Override
                    protected void done() {
                        try {
                            tasksPanel.upsertTask(get());
                        } catch (Exception ignored) {
                        }
                    }
                }.execute();
                break;
            case "TASK_DELETED":
                tasksPanel.removeTaskById(taskId);
                break;
            default:
                break;
        }
    }
}
