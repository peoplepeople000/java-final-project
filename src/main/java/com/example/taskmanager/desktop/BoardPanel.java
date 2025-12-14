package com.example.taskmanager.desktop;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

public class BoardPanel extends JPanel {

    private final DesktopApiClient apiClient;
    private final ProjectsListPanel projectsPanel;
    private final TasksListPanel tasksPanel;
    private final Runnable logoutAction;
    private final JLabel userLabel = new JLabel();
    private final JButton refreshAllButton = new JButton("Refresh");
    private final ChangesPoller changesPoller;

    public BoardPanel(DesktopApiClient apiClient, Runnable logoutAction) {
        this.apiClient = apiClient;
        this.logoutAction = logoutAction;
        this.projectsPanel = new ProjectsListPanel(apiClient);
        this.tasksPanel = new TasksListPanel(apiClient);
        this.changesPoller = new ChangesPoller(apiClient, projectsPanel, tasksPanel);

        setLayout(new BorderLayout());
        add(buildHeader(), BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, projectsPanel, tasksPanel);
        split.setResizeWeight(0.25);
        add(split, BorderLayout.CENTER);

        projectsPanel.setProjectSelectionListener(project -> tasksPanel.setCurrentProject(project));
        projectsPanel.setProjectClearedListener(tasksPanel::clearCurrentProject);
    }

    private JPanel buildHeader() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton logoutBtn = new JButton("Logout");
        logoutBtn.addActionListener(e -> {
            changesPoller.stop();
            changesPoller.reset();
            if (logoutAction != null) {
                logoutAction.run();
            }
        });
        panel.add(new JLabel("Logged in as "));
        panel.add(userLabel);
        refreshAllButton.setToolTipText("Force reload projects and tasks");
        refreshAllButton.addActionListener(e -> doRefreshAll());
        panel.add(refreshAllButton);
        panel.add(logoutBtn);
        return panel;
    }

    public void onShow() {
        DesktopApiClient.AuthResponse user = apiClient.getCurrentUser();
        userLabel.setText(user != null ? user.getUsername() + " (" + user.getEmail() + ")" : "");
        projectsPanel.reloadProjects();
        changesPoller.start();
        tasksPanel.setCurrentProject(null);
    }

    private void doRefreshAll() {
        refreshAllButton.setEnabled(false);
        Long projectId = projectsPanel.getSelectedProjectId();
        Long taskId = tasksPanel.getSelectedTaskId();
        new javax.swing.SwingWorker<RefreshBundle, Void>() {
            @Override
            protected RefreshBundle doInBackground() {
                RefreshBundle bundle = new RefreshBundle();
                bundle.projects = apiClient.listProjects();
                bundle.selectedProjectId = projectId;
                if (projectId != null) {
                    bundle.members = apiClient.listProjectMembers(projectId);
                    bundle.tasks = apiClient.listTasks(projectId);
                    bundle.selectedTaskId = taskId;
                }
                return bundle;
            }

            @Override
            protected void done() {
                refreshAllButton.setEnabled(true);
                try {
                    RefreshBundle bundle = get();
                    projectsPanel.replaceAllProjects(bundle.projects, bundle.selectedProjectId);
                    if (bundle.selectedProjectId != null && bundle.members != null) {
                        projectsPanel.replaceMembers(bundle.members);
                    }
                    if (bundle.selectedProjectId != null && bundle.tasks != null) {
                        tasksPanel.replaceAllTasks(bundle.tasks, bundle.selectedTaskId);
                    } else {
                        tasksPanel.clearCurrentProject();
                    }
                } catch (Exception ex) {
                    // simple fallback: ignore silently; could show status label if desired
                }
            }
        }.execute();
    }

    private static class RefreshBundle {
        java.util.List<DesktopApiClient.ProjectDto> projects;
        java.util.List<DesktopApiClient.MemberDto> members;
        java.util.List<DesktopApiClient.TaskDto> tasks;
        Long selectedProjectId;
        Long selectedTaskId;
    }
}
