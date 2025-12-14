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

    public BoardPanel(DesktopApiClient apiClient, RealtimeUpdateClient realtimeUpdateClient, Runnable logoutAction) {
        this.apiClient = apiClient;
        this.logoutAction = logoutAction;
        this.projectsPanel = new ProjectsListPanel(apiClient, realtimeUpdateClient);
        this.tasksPanel = new TasksListPanel(apiClient, realtimeUpdateClient);

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
            if (logoutAction != null) {
                logoutAction.run();
            }
        });
        panel.add(new JLabel("Logged in as "));
        panel.add(userLabel);
        panel.add(logoutBtn);
        return panel;
    }

    public void onShow() {
        DesktopApiClient.AuthResponse user = apiClient.getCurrentUser();
        userLabel.setText(user != null ? user.getUsername() + " (" + user.getEmail() + ")" : "");
        projectsPanel.reloadProjects();
        tasksPanel.setCurrentProject(null);
    }
}
